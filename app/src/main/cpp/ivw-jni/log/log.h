//
// Created by huang on 2019/1/25.
//

#ifndef IVWENGINEDEMO_LOG_H
#define IVWENGINEDEMO_LOG_H

#include <android/log.h>

#define LOG_TAG "ivw"

#define LOG_D(...) do {\
    if (isLogOn()) {\
        __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__);\
    }\
} while(false)

#define LOG_E(...) do {\
    if (true) { \
        __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__); \
    } \
} while (false)

bool isLogOn();

void setLog(bool isOn);

#endif //IVWENGINEDEMO_LOG_H
