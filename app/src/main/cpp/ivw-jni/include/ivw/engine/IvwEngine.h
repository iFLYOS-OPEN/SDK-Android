//
// Created by huang on 2019/1/24.
//

#ifndef __IVWENGINE_H__
#define __IVWENGINE_H__

#include "libivw_defines.h"

#if defined(_MSC_VER)
#if !defined(IVWAPI)
#define IVWAPI _cdecl
#endif
#pragma pack(push, 8)
#else
#if !defined(IVWAPI)
//#define IVWAPI __attribute__((visibility("default")))
#define IVWAPI
#endif
#endif

typedef void* IVW_INSTHANDLE;

typedef struct tagIVW_RES_SET
{
    int nResId;
    char strType[16];
} IVW_RES_SET;

enum IVW_RES_LOCATION
{
    IVW_RES_LOCATION_FILE   = 0,
    IVW_RES_LOCATION_MEM    = 1,
    IVW_RES_LOCATION_NONE
};

typedef int (*PWIVWAPIWRAPCallBack)(void* pUserParam, const char* pIvwParam);

struct IVWEngineFace
{
    virtual int IvwInit(const char* pEngine, void* pReserved) = 0;

    virtual int IvwFini() = 0;

    virtual int IvwLoadResource(char* pRes, int nResSize, int bResFlag) = 0;

    virtual int IvwCreateInst(IVW_INSTHANDLE* ppIvwInst) = 0;

    virtual int IvwDestroyInst(IVW_INSTHANDLE pIvwInst) = 0;

    virtual int IvwGetInstParam(IVW_INSTHANDLE pIvwInst, int nParamType, void* nParamValue, int* nParamSize) = 0;

    virtual int IvwSetInstParam(IVW_INSTHANDLE pIvwInst, int nParamType, void* nParamValue, int nParamSize) = 0;

    virtual int IvwStartInst(IVW_INSTHANDLE pIvwInst) = 0;

    virtual int IvwWriteInst(IVW_INSTHANDLE pIvwInst, char* pBuf, int nBufSize) = 0;

    virtual int IvwStopInst(IVW_INSTHANDLE pIvwInst) = 0;

    virtual int IvwGetVersion(char* pIvwVersion, int* nVerLen) = 0;
};

typedef IVWEngineFace* PIVWEngineFace;

#ifdef __cplusplus
extern "C" {
#endif

int IVWAPI CreateIVWEngine(void* pParam, PIVWEngineFace* ppEngineFace);
typedef int (IVWAPI* Proc_CreateIVWEngine)(void* pParam, PIVWEngineFace* ppEngineFace);

int IVWAPI DestroyIVWEngine(PIVWEngineFace pEngineFace);
typedef int (IVWAPI* Proc_DestroyIVWEngine)(PIVWEngineFace pEngineFace);

#ifdef __cplusplus
};
#endif

#endif //__IVWENGINE_H__
