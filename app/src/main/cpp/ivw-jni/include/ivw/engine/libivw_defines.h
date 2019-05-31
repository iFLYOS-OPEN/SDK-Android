//
// Created by huang on 2019/1/24.
//

#ifndef __LIBIVW_DEFINES_H__
#define __LIBIVW_DEFINES_H__

#define IVW_PARAM_LOCAL_BASE 0x0000

enum
{
    IVW_PARAM_WAKEUPCALLBACK            = (IVW_PARAM_LOCAL_BASE + 1),
    IVW_PARAM_CM_LEVEL                  = (IVW_PARAM_LOCAL_BASE + 2),
    IVW_PARAM_RESULT_CB_USERPARAM       = (IVW_PARAM_LOCAL_BASE + 3),
    IVW_PARAM_KEYWORD_NCM               = (IVW_PARAM_LOCAL_BASE + 4)
};

enum
{
    IVW_ERROR_SUCCESS                   = (0),
    IVW_ERROR_INVALID_HANDLE            = (1),
    IVW_ERROR_ALREADY_INIT              = (2),
    IVW_ERROR_INVALID_PARA_VALUE        = (3),
    IVW_ERROR_FAIL                      = (4),
    IVW_ERROR_NOT_INIT                  = (5),
    IVW_ERROR_INVALID_PARA_TYPE         = (6),
    IVW_ERROR_BUF_OVERFLOW              = (7),
    IVW_ERROR_LOAD_DLL                  = (8)
};

enum
{
    IVW_CMLevel_Lowest                  = 0,
    IVW_CMLevel_Lower                   = 1,
    IVW_CMLevel_Low                     = 2,
    IVW_CMLevel_Normal                  = 3,
    IVW_CMLevel_high                    = 4,
    IVW_CMLevel_higher                  = 5,
    IVW_CMLevel_highest                 = 6
};

#endif //__LIBIVW_DEFINES_H__
