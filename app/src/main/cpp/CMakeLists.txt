cmake_minimum_required(VERSION 3.22.1)
project(baml_jni C)

add_library(bridge_cffi SHARED IMPORTED)
set_target_properties(bridge_cffi PROPERTIES
    IMPORTED_LOCATION ${CMAKE_SOURCE_DIR}/../../../build/generated/baml-jniLibs/${ANDROID_ABI}/libbridge_cffi.so
    IMPORTED_NO_SONAME TRUE
)

add_library(baml_jni SHARED baml_jni.c)

target_link_libraries(baml_jni
    bridge_cffi
    log
)
