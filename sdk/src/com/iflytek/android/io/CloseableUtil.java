package com.iflytek.android.io;

import java.io.Closeable;

public class CloseableUtil {

    public static void safeClose(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (Exception ignored) {
            }
        }
    }

}
