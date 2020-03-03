package com.example.android.camera2video.listener;

public interface DecisionListener {
    /**
     * 舍弃
     */
    void record();

    /**
     * 使用
     */
    void use(String videoPath);
}
