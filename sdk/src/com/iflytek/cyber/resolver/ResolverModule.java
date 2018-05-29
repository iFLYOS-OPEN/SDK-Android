package com.iflytek.cyber.resolver;

import android.content.Context;

import com.iflytek.cyber.CyberDelegate;

public abstract class ResolverModule implements Resolver {

    protected final Context context;
    protected final CyberDelegate delegate;

    public ResolverModule(Context context, CyberDelegate delegate) {
        this.context = context;
        this.delegate = delegate;
    }

    public void onCreate() {
    }

    public void onDestroy() {
    }

    public void updateContext() {
    }

    public void onCancel() {
    }

}
