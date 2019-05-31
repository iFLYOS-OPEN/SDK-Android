//
// Created by huang on 2019/1/25.
//
#include "log.h"

static bool gIsLogOn = true;

bool isLogOn()
{
    return gIsLogOn;
}

void setLog(bool isOn)
{
    gIsLogOn = isOn;
}