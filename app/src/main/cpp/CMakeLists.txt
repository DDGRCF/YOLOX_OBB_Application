# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

# Sets the minimum version of CMake required to build the native library.

cmake_minimum_required(VERSION 3.10.0)

# Declares and names the project.

project("yoloxobbncnn")

#set(ANDROID_ABI arm64-v8a)
#set(SUPPORT_ANDROID_API "arm64-v8a" "armeabi-v7a", "x86", "x86_64")
#list(FIND SUPPORT_ANDROID_API ${ANDROID_ABI} FIND_OR_NOT)
#if (${FIND_OR_NOT} EQUAL "-1")
#    message(FATAL_ERROR "Unsupport ${ANDROID_ABI} ABI VERSION")
#else()
#    message(STATUS "Find support android api: ${ANDROID_ABI}")
#endif()

set(ncnn_DIR ${CMAKE_SOURCE_DIR}/ncnn-20220729-android-vulkan/${ANDROID_ABI}/lib/cmake/ncnn)
find_package(ncnn REQUIRED)

add_library(${CMAKE_PROJECT_NAME} SHARED yoloxobbncnn_jni.cpp)

find_library( # Sets the name of the path variable.
        log-lib
        log)


target_link_libraries( # Specifies the target library.
        ${CMAKE_PROJECT_NAME}
        ncnn
        jnigraphics
        # Links the target library to the log library
        # included in the NDK.
        ${log-lib})