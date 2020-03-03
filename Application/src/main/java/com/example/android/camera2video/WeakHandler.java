package com.example.android.camera2video;

import android.os.Handler;

import java.lang.ref.WeakReference;

public abstract class WeakHandler<T> extends Handler {
    private WeakReference<T> mOwner;

    public WeakHandler(T owner) {
        this.mOwner = new WeakReference<T>(owner);
    }

    public T getOwner() {
        return mOwner.get();
    }
}
