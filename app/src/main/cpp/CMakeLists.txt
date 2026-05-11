cmake_minimum_required(VERSION 3.22.1)
project(baml_jni C)

# ---- Pre-built Rust library (libbridge_cffi.so) ----
# Located at app/src/main/jniLibs/<abi>/libbridge_cffi.so
# CMAKE_SOURCE_DIR = app/src/main/cpp, so ../jniLibs is the right relative path

add_library(bridge_cffi SHARED IMPORTED)
set_target_properties(bridge_cffi PROPERTIES
    IMPORTED_LOCATION ${CMAKE_SOURCE_DIR}/../jniLibs/${ANDROID_ABI}/libbridge_cffi.so
    IMPORTED_NO_SONAME TRUE
)

# ---- JNI bridge library ----
add_library(baml_jni SHARED baml_jni.c)

target_link_libraries(baml_jni
    bridge_cffi
    log
)
