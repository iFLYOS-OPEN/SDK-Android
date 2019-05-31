#include <jni.h>
#include <string>
#include <map>

#include "com_iflytek_cyber_iot_show_core_ivw_IVWEngine.h"
#include "include/ivw/engine/IvwEngine.h"
#include "log/log.h"
#include "file/FileUtil.h"

using namespace iflytek;
using namespace std;

#define INVALID_HANDLE 0


// 唤醒回调方法签名
const char* SIGNATURE_WAKEUP_CB = "(Ljava/lang/String;)V";

// 唤醒资源
struct IvwRes {
    char* buffer;

    size_t size;

    IvwRes(size_t nSize) : buffer(nullptr), size(nSize) {
        buffer = new char[nSize];
    }

    void release() {
        if (nullptr != buffer) {
            delete[] buffer;
            size = 0;

            buffer = nullptr;
        }
    }
};

// 引擎上下文
struct EngineContext {
    // 引擎接口
    PIVWEngineFace engineFace;

    // 唤醒资源
    IvwRes* ivwRes;

    // 唤醒实例指针
    IVW_INSTHANDLE instHandle;

    // JVM
    JavaVM* jvm;

    // java对象引用
    jobject objRef;

    // java对象方法id
    jmethodID wakeupCbMethodId;

    EngineContext() : engineFace(nullptr),
                  ivwRes(nullptr),
                  instHandle(nullptr),
                  jvm(nullptr),
                  objRef(nullptr),
                  wakeupCbMethodId(nullptr) {

    }

    EngineContext(PIVWEngineFace face,
        IvwRes* res,
        IVW_INSTHANDLE handle,
        JavaVM* vm,
        jobject ref,
        jmethodID methodID) : engineFace(face),
                  ivwRes(res),
                  instHandle(handle),
                  jvm(vm),
                  objRef(ref),
                  wakeupCbMethodId(methodID) {

    }
};

int wakeupCbFunc(void* pUserParam, const char* pIvwParam)
{
    LOG_D("wakeup result=%s", pIvwParam);

    if (nullptr == pUserParam) {
        LOG_E("user param is null");
        return -1;
    }

    EngineContext* context = (EngineContext*) pUserParam;
    JNIEnv* pEnv = nullptr;

    if ((context->jvm)->GetEnv((void**) &pEnv, JNI_VERSION_1_4) != JNI_OK) {
        LOG_E("get current jni env failed");
        return -1;
    }

    string resultStr = pIvwParam;
    jstring result = pEnv->NewStringUTF(resultStr.c_str());

    pEnv->CallVoidMethod(context->objRef, context->wakeupCbMethodId, result);

    return 0;
}

int loadIvwResource(PIVWEngineFace engineFace, JNIEnv *pEnv, jstring &resPath)
{
    if (nullptr == engineFace) {
        LOG_E("engine is null");
        return -1;
    }

    const char* resPathChar = pEnv->GetStringUTFChars(resPath, 0);
    string resPathStr = resPathChar;
    pEnv->ReleaseStringUTFChars(resPath, resPathChar);

    bool isFile = true;
    if (!FileUtil::isPathExist(resPathStr, isFile)) {
        LOG_E("path %s not exist", resPathStr.c_str());
        return -1;
    } else {
        if (!isFile) {
            LOG_E("path %s is not file", resPathStr.c_str());
            return -1;
        }
    }

    long resSize = FileUtil::getFileSizeInBytes(resPathStr);
    if (resSize <= 0) {
        LOG_E("%s is empty", resPathStr.c_str());
        return -1;
    }

    IvwRes* res = new IvwRes(resSize);
    long readCount = FileUtil::readFileToBuffer(resPathStr, res->buffer, resSize);
    if (readCount != resSize) {
        LOG_E("read %s failed", resPathStr.c_str());

        res->release();
        delete res;

        return -1;
    }

    char* resBuffer = res->buffer;
    if (resBuffer[0] == 'I' && resBuffer[3] == 'Y' && resBuffer[6] == 'K') {
        int ret = engineFace->IvwLoadResource(resBuffer, res->size, 1);
        if (0 != ret) {
            LOG_E("load %s failed, ret=%d", resPathStr.c_str(), ret);

            res->release();
            delete res;

            return -1;
        } else {
            LOG_D("load %s success", resPathStr.c_str());
        }
    } else {

    }

    if (nullptr != res) {
        res->release();
        delete res;
    }

    return 0;
}

PIVWEngineFace createAndInitEngine()
{
    PIVWEngineFace face = nullptr;
    int ret = CreateIVWEngine(nullptr, &face);
    if (IVW_ERROR_SUCCESS != ret) {
        LOG_E("create engine failed, ret=%d", ret);
    } else {
        face->IvwInit(nullptr, nullptr);

        LOG_D("create engine success");
    }

    return face;
}

JNIEXPORT jlong JNICALL Java_com_iflytek_cyber_iot_show_core_ivw_IVWEngine_jni_1create
        (JNIEnv* pEnv, jobject obj, jstring resPath, jstring wakeupCbName)
{
    PIVWEngineFace engineFace = createAndInitEngine();
    if (nullptr == engineFace) {
        LOG_E("engine is null");
        return INVALID_HANDLE;
    }

    LOG_D("create engine success");

    int ret = loadIvwResource(engineFace, pEnv, resPath);
    if (-1 == ret) {
        LOG_E("load ivw resource failed");

        DestroyIVWEngine(engineFace);
        return INVALID_HANDLE;
    }

    IVW_INSTHANDLE instHandle = nullptr;
    ret = engineFace->IvwCreateInst(&instHandle);
    if (IVW_ERROR_SUCCESS != ret) {
        LOG_E("create instance failed, ret=%d", ret);

        DestroyIVWEngine(engineFace);

        return INVALID_HANDLE;
    } else {
        LOG_D("create engine instance success");
    }

    JavaVM* vm;
    pEnv->GetJavaVM(&vm);
    jobject ref = pEnv->NewGlobalRef(obj);

    jclass engineCls = pEnv->GetObjectClass(obj);
    const char* wakeupCbNameChar = pEnv->GetStringUTFChars(wakeupCbName, 0);
    jmethodID methodId = pEnv->GetMethodID(engineCls, wakeupCbNameChar,
                                           SIGNATURE_WAKEUP_CB);

    if (nullptr == methodId) {
        LOG_E("can't find method %s with signature %s", wakeupCbNameChar, SIGNATURE_WAKEUP_CB);
        pEnv->ReleaseStringUTFChars(wakeupCbName, wakeupCbNameChar);

        DestroyIVWEngine(engineFace);
        pEnv->DeleteGlobalRef(ref);

        return INVALID_HANDLE;
    }

    EngineContext* engineEnv = new EngineContext(engineFace, nullptr, instHandle, vm, ref, methodId);
    pEnv->ReleaseStringUTFChars(wakeupCbName, wakeupCbNameChar);

    // 设置用户参数，唤醒回调时会带上
    engineFace->IvwSetInstParam(engineEnv->instHandle,
                                     IVW_PARAM_RESULT_CB_USERPARAM,
                                     (void*) engineEnv,
                                     sizeof(void*));

    // 设置唤醒回调
    engineFace->IvwSetInstParam(engineEnv->instHandle,
                                     IVW_PARAM_WAKEUPCALLBACK,
                                     (void*) wakeupCbFunc,
                                     sizeof(void*));

    return (jlong) engineEnv;
}

JNIEXPORT void JNICALL Java_com_iflytek_cyber_iot_show_core_ivw_IVWEngine_jni_1destroy
        (JNIEnv* pEnv, jclass thiz, jlong handle)
{
    if (INVALID_HANDLE == handle) {
        LOG_E("invalid engine env handle, destroy failed");
        return;
    }

    EngineContext* context = (EngineContext*) handle;
    (context->engineFace)->IvwDestroyInst(context->instHandle);
    (context->engineFace)->IvwFini();

    DestroyIVWEngine(context->engineFace);
    pEnv->DeleteGlobalRef(context->objRef);

    delete context;

    LOG_D("engine destroyed");
}

JNIEXPORT jint JNICALL Java_com_iflytek_cyber_iot_show_core_ivw_IVWEngine_jni_1set_1cmlevel
        (JNIEnv* pEnv, jclass thiz, jlong handle, jint level)
{
    if (INVALID_HANDLE == handle) {
        LOG_E("invalid engine context, set CMLevel failed");
        return -1;
    }

    EngineContext* context = (EngineContext*) handle;

    return (context->engineFace)->IvwSetInstParam(context->instHandle,
                IVW_PARAM_CM_LEVEL, &level, sizeof(int));
}

JNIEXPORT jint JNICALL Java_com_iflytek_cyber_iot_show_core_ivw_IVWEngine_jni_1set_1keywordncm
        (JNIEnv* pEnv, jclass thiz, jlong handle, jstring ncm)
{
    if (INVALID_HANDLE == handle) {
        LOG_E("invalid engine context, set keyword ncm failed");
        return -1;
    }

    const char* ncmChar = pEnv->GetStringUTFChars(ncm, 0);

    EngineContext* context = (EngineContext*) handle;
    int ret = (context->engineFace)->IvwSetInstParam(context->instHandle,
                    IVW_PARAM_KEYWORD_NCM, (void*) ncmChar, strlen(ncmChar));

    pEnv->ReleaseStringUTFChars(ncm, ncmChar);

    return ret;
}

JNIEXPORT jint JNICALL Java_com_iflytek_cyber_iot_show_core_ivw_IVWEngine_jni_1start
        (JNIEnv* pEnv, jclass thiz, jlong handle)
{
    int ret = -1;
    if (INVALID_HANDLE == handle) {
        LOG_E("invalid engine context, start failed");
        return ret;
    }

    EngineContext* context = (EngineContext*) handle;
    ret = (context->engineFace)->IvwStartInst((IVW_INSTHANDLE) context->instHandle);

    LOG_D("engine instance started");

    return ret;
}

JNIEXPORT jint JNICALL Java_com_iflytek_cyber_iot_show_core_ivw_IVWEngine_jni_1write
        (JNIEnv* pEnv, jclass thiz, jlong handle, jbyteArray buffer, jint size)
{
    int ret = -1;
    if (INVALID_HANDLE == handle) {
        LOG_E("invalid engine context, write failed");
        return ret;
    }

    EngineContext* context = (EngineContext*) handle;
    char* bufferChar = (char*) pEnv->GetByteArrayElements(buffer, 0);
    ret = (context->engineFace)->IvwWriteInst((IVW_INSTHANDLE) context->instHandle, bufferChar, size);
    pEnv->ReleaseByteArrayElements(buffer, (jbyte*) bufferChar, 0);

    return ret;
}

JNIEXPORT jint JNICALL Java_com_iflytek_cyber_iot_show_core_ivw_IVWEngine_jni_1stop
        (JNIEnv* pEnv, jclass thiz, jlong handle)
{
    int ret = -1;
    if (INVALID_HANDLE == handle) {
        LOG_E("invalid engine context, stop failed");
        return ret;
    }

    EngineContext* context = (EngineContext*) handle;
    ret = (context->engineFace)->IvwStopInst((IVW_INSTHANDLE) context->instHandle);

    LOG_D("engine instance stopped");

    return ret;
}

JNIEXPORT void JNICALL Java_com_iflytek_cyber_iot_show_core_ivw_IVWEngine_jni_1set_1log
        (JNIEnv* pEnv, jclass thiz, jboolean isOn)
{
    setLog(isOn);
}

JNIEXPORT jstring JNICALL Java_com_iflytek_cyber_iot_show_core_ivw_IVWEngine_jni_1get_1version
        (JNIEnv* pEnv, jclass thiz, jlong handle)
{
    if (INVALID_HANDLE == handle) {
        LOG_E("invalid engine context, get vesion failed");
        return nullptr;
    }

    EngineContext* context = (EngineContext*) handle;

    int len = 100;
    char version[100] = {0};
    context->engineFace->IvwGetVersion(version, &len);

    return pEnv->NewStringUTF(version);
}
