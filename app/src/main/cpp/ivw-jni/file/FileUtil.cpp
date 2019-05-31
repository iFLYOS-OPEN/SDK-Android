//
// Created by huang on 2019/1/24.
//

#include "FileUtil.h"

#include <unistd.h>
#include <stdio.h>
#include <sys/stat.h>
#include <fstream>

namespace iflytek {

bool FileUtil::isPathExist(const string& path, bool& isFile)
{
    if (path.empty()) {
        return false;
    }

    bool exist = false;
    if (access(path.c_str(), F_OK) != -1) {
        exist = true;

        struct stat buf;
        if (0 == stat(path.c_str(), &buf)) {
            isFile = !S_ISDIR(buf.st_mode);
        }
    }

    return exist;
}

long FileUtil::getFileSizeInBytes(const string& path)
{
    struct stat buf;
    if (0 == stat(path.c_str(), &buf)) {
        return buf.st_size;
    }

    return -1;
}

long FileUtil::readFileToBuffer(const string& path, char* buffer, long bufferSize)
{
    fstream ins;

    ins.open(path, ios::in | ios::binary);
    if (ins.is_open()) {
        ins.read(buffer, bufferSize);

        return ins.gcount();
    }

    return -1;
}

}