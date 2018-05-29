package com.iflytek.cyber.resolver.audioplayer;

import com.iflytek.cyber.CyberDelegate;

public class DelegateStore {
    private static DelegateStore instance;

    private CyberDelegate delegate;

    private DelegateStore() {

    }

    public static DelegateStore get() {
        if (instance == null)
            instance = new DelegateStore();
        return instance;
    }

    public void setup(CyberDelegate delegate) {
        this.delegate = delegate;
    }

    public CyberDelegate getDelegate() {
        return delegate;
    }
}
