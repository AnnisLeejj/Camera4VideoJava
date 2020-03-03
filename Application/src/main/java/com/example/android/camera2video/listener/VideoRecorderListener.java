package com.example.android.camera2video.listener;

public interface VideoRecorderListener {
    void compare(String videoPath);

    /**
     * 通知activity管理 横竖屏
     */
    void recording(int recording);

//    /**
//     * 通知activity管理 横竖屏
//     */
//    void cancel();
}
