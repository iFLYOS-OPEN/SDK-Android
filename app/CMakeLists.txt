cmake_minimum_required(VERSION 3.4.1)


link_directories(${PROJECT_SOURCE_DIR}/src/main/jniLibs/${ANDROID_ABI})

find_library(log-lib log)

add_library(ivw-jni SHARED ${CMAKE_SOURCE_DIR}/src/main/cpp/ivw-jni/ivw-jni.cpp
    ${CMAKE_SOURCE_DIR}/src/main/cpp/ivw-jni/file/FileUtil.cpp
    ${CMAKE_SOURCE_DIR}/src/main/cpp/ivw-jni/log/log.cpp)

target_link_libraries(ivw-jni
    ${log-lib}
    -lIvw60
)
