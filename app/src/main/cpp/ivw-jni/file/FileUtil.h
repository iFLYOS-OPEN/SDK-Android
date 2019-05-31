//
// Created by huang on 2019/1/24.
//

#ifndef IVWENGINEDEMO_FILEUTIL_H
#define IVWENGINEDEMO_FILEUTIL_H

#include <string>

using namespace std;

namespace iflytek {

class FileUtil {
public:
    static bool isPathExist(const string& path, bool& isFile);

    static long getFileSizeInBytes(const string& path);

    static long readFileToBuffer(const string& path, char* buffer, long bufferSize);
};

}


#endif //IVWENGINEDEMO_FILEUTIL_H
