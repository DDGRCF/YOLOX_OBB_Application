#include "layer.h"
#include "net.h"
#include "benchmark.h"

#include <android/asset_manager_jni.h>
#include <android/bitmap.h>
#include <android/log.h>

#include <jni.h>
#include <string>

#include <string>
#include <vector>

#include "obb_nms.hpp"

static ncnn::UnlockedPoolAllocator g_blob_pool_allocator;
static ncnn::PoolAllocator g_workspace_pool_allocator;

static ncnn::Net yolox_obb;
#define YOLOX_TAG "YoloxObbNcnn"

struct Object {
    Object() = default;
    Object(float _x, float _y, float _w, float _h, float _t, int _l, float _p)
        : x(_x), y(_y), w(_w), h(_h), theta(_t), label(_l), prob(_p) {}
    float x;
    float y;
    float w;
    float h;
    float theta;
    int label;
    float prob;
};

struct Parameter {
    float conf_thre;
    float nms_thre;
    bool agnostic;
    bool use_gpu;
};


void qsort_descent_inplace(std::vector<Object>& objects, int left, int right)
{
    int i = left;
    int j = right;
    float p = objects[(left + right) / 2].prob;

    while (i <= j) {
        while (objects[i].prob > p) i++;

        while (objects[j].prob < p) j--;

        if (i <= j) {
            // swap
            std::swap(objects[i], objects[j]);

            i++;
            j--;
        }
    }

#pragma omp parallel sections
    {
#pragma omp section
        {
            if (left < j) qsort_descent_inplace(objects, left, j);
        }
#pragma omp section
        {
            if (i < right) qsort_descent_inplace(objects, i, right);
        }
    }
}

void qsort_descent_inplace(std::vector<Object>& objects) {
    if (objects.empty()) return;

    qsort_descent_inplace(objects, 0, objects.size() - 1);
}

void nms_sorted_bboxes(const std::vector<Object>& objects, std::vector<int>& picked, float nms_thre, bool agnostic) {
    picked.clear();

    const int n = (int)objects.size();

    float iou{0};
    for (int i = 0; i < n; i++) {
        const Object& a = objects[i];

        bool keep = true;
        for (int j = 0; j < (int)picked.size(); j++) {
            const Object& b = objects[picked[j]];

            if (!agnostic && a.label != b.label) continue;

            float box1[5]; float box2[5];
            box1[0] = a.x; box1[1] = a.y; box1[2] = a.w; box1[3] = a.h;
            box1[4] = (float) (-a.theta * M_PI / 180.);

            box2[0] = b.x; box2[1] = b.y; box2[2] = b.w; box2[3] = b.h;
            box2[4] = (float) (-b.theta * M_PI / 180.);

            iou = single_box_iou_rotated<float>(box1, box2);
            if (iou > nms_thre) keep = false;
        }

        if (keep)
            picked.push_back(i);
    }
}

extern "C" {

static jclass obj_cls = nullptr;
static jmethodID obj_constructor_id;
static jfieldID x_id;
static jfieldID y_id;
static jfieldID w_id;
static jfieldID h_id;
static jfieldID theta_id;
static jfieldID label_id;
static jfieldID prob_id;


static jclass param_cls = nullptr;
static jmethodID param_constructor_id;
static jfieldID conf_thre_id;
static jfieldID nms_thre_id;
static jfieldID agnostic_id;
static jfieldID use_gpu_id;

JNIEXPORT jint
JNI_OnLoad(JavaVM *vm, void *reserved) {
    __android_log_print(ANDROID_LOG_DEBUG, YOLOX_TAG, "JNI_OnLoad");

    ncnn::create_gpu_instance();

    return JNI_VERSION_1_6;
}

JNIEXPORT void
JNI_OnUnload(JavaVM *vm, void *reserved) {
    __android_log_print(ANDROID_LOG_DEBUG, YOLOX_TAG, "JNI_OnUnload");

    ncnn::destroy_gpu_instance();
}

JNIEXPORT jboolean JNICALL
Java_com_github_ddgrcf_yolox_1demo_YoloxObbNcnn_Init(JNIEnv *env, jobject self,
                                                     jobject assetManager) {
    // common option
    ncnn::Option opt;
    opt.lightmode = true;
    opt.num_threads = 4;
    opt.blob_allocator = &g_blob_pool_allocator;
    opt.workspace_allocator = &g_workspace_pool_allocator;
    opt.use_packing_layout = true;

    if (ncnn::get_gpu_count() != 0) opt.use_vulkan_compute = true;

    AAssetManager *mgr = AAssetManager_fromJava(env, assetManager);
    yolox_obb.opt = opt;

    // load model
    {
        int ret = yolox_obb.load_param(mgr, "yolox_s_dota1_0_deploy_new.param");
        if (ret != 0) {
            __android_log_print(ANDROID_LOG_DEBUG, YOLOX_TAG, "load_param fail!");
            return JNI_FALSE;
        }
    }

    {
        int ret = yolox_obb.load_model(mgr, "yolox_s_dota1_0_deploy_new.bin");
        if (ret != 0) {
            __android_log_print(ANDROID_LOG_DEBUG, YOLOX_TAG, "load_bin fail!");
            return JNI_FALSE;
        }
    }

    // Obj
    jclass local_objects = env->FindClass("com/github/ddgrcf/yolox_demo/YoloxObbNcnn$Obj");
    obj_cls = reinterpret_cast<jclass>(env->NewGlobalRef(local_objects));

    obj_constructor_id = env->GetMethodID(obj_cls, "<init>", "()V");
    x_id = env->GetFieldID(obj_cls, "x", "F");
    y_id = env->GetFieldID(obj_cls, "y", "F");
    w_id = env->GetFieldID(obj_cls, "w", "F");
    h_id = env->GetFieldID(obj_cls, "h", "F");
    theta_id = env->GetFieldID(obj_cls, "theta", "F");
    label_id = env->GetFieldID(obj_cls, "label", "I");
    prob_id = env->GetFieldID(obj_cls, "prob", "F");

    // Param
    jclass local_params = env->FindClass("com/github/ddgrcf/yolox_demo/YoloxObbNcnn$Param");
    param_cls = reinterpret_cast<jclass>(env->NewGlobalRef(local_params));

    param_constructor_id = env->GetMethodID(param_cls, "<init>", "()V");
    conf_thre_id = env->GetFieldID(param_cls, "confScoreThreshold", "F");
    nms_thre_id = env->GetFieldID(param_cls, "nmsIoUThreshold", "F");
    agnostic_id = env->GetFieldID(param_cls, "agnostic", "Z");
    use_gpu_id = env->GetFieldID(param_cls, "useGpu", "Z");

    return JNI_TRUE;
}

JNIEXPORT jobjectArray JNICALL
Java_com_github_ddgrcf_yolox_1demo_YoloxObbNcnn_Detect(JNIEnv *env, jobject self, jobject bitmap,
                                                       jobject param) {
    Parameter parameter{};
    parameter.conf_thre = env->GetFloatField(param, conf_thre_id);
    parameter.nms_thre = env->GetFloatField(param, nms_thre_id);
    parameter.agnostic = env->GetBooleanField(param, agnostic_id);
    parameter.use_gpu = env->GetBooleanField(param, use_gpu_id);

    if (parameter.use_gpu == JNI_TRUE && ncnn::get_gpu_count() == 0) return nullptr;

    double start_time = ncnn::get_current_time();

    AndroidBitmapInfo  info;
    AndroidBitmap_getInfo(env, bitmap, &info);
    const int width = info.width;
    const int height = info.height;
    if (info.format != ANDROID_BITMAP_FORMAT_RGBA_8888) return nullptr;

    const int target_size = 1024;

    int w = width;
    int h = height;
    float scale = 1.f;

    if (w > h) {
        scale = (float)target_size / w;
        w = target_size;
        h = h * scale;
    }
    else {
        scale = (float)target_size / h;
        h = target_size;
        w = w * scale;
    }

    std::vector<Object> objects;
    {
        ncnn::Mat in = ncnn::Mat::from_android_bitmap_resize(env, bitmap, ncnn::Mat::PIXEL_RGB, w, h);
        int wpad = (w + 31) / 32 * 32 - w;
        int hpad = (h + 31) / 32 * 32 - h;
        ncnn::Mat in_pad;
        ncnn::copy_make_border(in, in_pad, hpad / 2, hpad - hpad / 2, wpad / 2, wpad - wpad / 2, ncnn::BORDER_CONSTANT, 114.f);
        ncnn::Extractor ex = yolox_obb.create_extractor();

        ncnn::Mat scores_out;
        ncnn::Mat class_out;
        ncnn::Mat boxes_out;

        ex.input("input", in_pad);
        ex.extract("scores", scores_out);
        ex.extract("boxes", boxes_out);
        ex.extract("class", class_out);

        auto num_proposals = scores_out.total();
        auto num_classes = class_out.w;
        std::vector<Object> proposals;
        {
            for (auto y = 0; y < (long)num_proposals; y++) {
                float* scores_row = scores_out.row(y);
                float* boxes_row = boxes_out.row(y);
                float* class_row = class_out.row(y);
                auto prob = *scores_row;
                int class_index = std::max_element(class_row, class_row + num_classes) - class_row;
                auto class_score = class_row[class_index];
                prob *= (float)(prob * class_score);
                if (prob < parameter.conf_thre) continue;
                auto ctr_x = boxes_row[0]; auto ctr_y = boxes_row[1];
                if (ctr_x < 0. || ctr_y < 0. ||
                    ctr_x > target_size ||
                    ctr_y > target_size) continue;

                auto obj_w = boxes_row[2]; auto obj_h = boxes_row[3]; auto obj_t = boxes_row[4];
                obj_t = (float) (-obj_t * 180 / M_PI);

                proposals.emplace_back(
                        ctr_x, ctr_y, obj_w, obj_h, obj_t,
                        class_index, prob
                );
            }
        }

        std::vector<int> picked;
        {
            qsort_descent_inplace(proposals);
            nms_sorted_bboxes(proposals, picked, parameter.nms_thre, parameter.agnostic);
        }

        {
            auto keep_num = picked.size();
            objects.resize(keep_num);
            for (auto i = 0; i < (long)keep_num; i++) {
                objects[i] = proposals[picked[i]];
            }
        }

    }
    jobjectArray jObjArray = env->NewObjectArray((long)objects.size(), obj_cls, nullptr);

    for (auto i = 0; i < objects.size(); i++) {
        jobject jObj = env->NewObject(obj_cls, obj_constructor_id); // TODO: this
        env->SetFloatField(jObj, x_id, objects[i].x);
        env->SetFloatField(jObj, y_id, objects[i].y);
        env->SetFloatField(jObj, w_id, objects[i].w);
        env->SetFloatField(jObj, h_id, objects[i].h);
        env->SetFloatField(jObj, theta_id, objects[i].theta);
        env->SetIntField(jObj, label_id, objects[i].label);
        env->SetFloatField(jObj, prob_id, objects[i].prob);
        env->SetObjectArrayElement(jObjArray, i, jObj);
    }

    __android_log_print(ANDROID_LOG_DEBUG, YOLOX_TAG, "%.2fms detect", ncnn::get_current_time() - start_time);

    return jObjArray;
}

JNIEXPORT jobjectArray JNICALL
Java_com_github_ddgrcf_yolox_1demo_YoloxObbNcnn_Deal(JNIEnv *env, jobject thiz, jobject objects,
                                                     jobject param) {
    // TODO: implement Deal()
}

}