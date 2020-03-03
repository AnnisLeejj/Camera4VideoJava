package com.example.android.camera2video;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Build;
import android.os.Bundle;

import com.example.android.camera2video.listener.DecisionListener;
import com.example.android.camera2video.listener.VideoRecorderListener;
import com.example.android.camera2video.part.Camera2VideoFragment;
import com.example.android.camera2video.part.CameraVideoFragment;
import com.example.android.camera2video.part.PlayVideoFragment;

public class CameraActivity extends Activity implements VideoRecorderListener, DecisionListener {
    /**
     * EXTRA
     */
    public static final String EXTRA_SAVE_PATH = "EXTRA_SAVE_PATH";

    /**
     * STATE_VIEW
     */
    public static final String STATE_VIEW = "STATE_VIEW";
    /**
     * STATE_VIEW
     */
    public static final String STATE_PATH = "STATE_PATH";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        if (savedInstanceState != null) {
            currentView = savedInstanceState.getInt(STATE_VIEW);
            videoPath = savedInstanceState.getString(STATE_PATH);
            if (currentView == 0) {
                record();
            } else {
                getFragmentManager().beginTransaction()
                        .replace(R.id.container, PlayVideoFragment.newInstance(videoPath))
                        .commit();
            }
        } else {
            record();
        }
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    String videoPath;
    /**
     * 0 录像, 1 播放
     */
    int currentView = 0;

    @Override
    public void recording(int orientation1) {
        boolean aBoolean = getResources().getBoolean(R.bool.idLand);
        if (aBoolean) {
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O_MR1) {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_BEHIND);
            } else {
                int orientation = getWindowManager().getDefaultDisplay().getOrientation();
                //        横屏：
                if (orientation == 1) {
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                } else {
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
                }
            }
        } else {
            //        竖屏：
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
    }

    @Override
    public void compare(String videoPath) {
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        this.videoPath = videoPath;
//        Toast.makeText(this, "Video saved: " + videoPath,
//                Toast.LENGTH_SHORT).show();
        currentView = 1;
        getFragmentManager().beginTransaction()
                .replace(R.id.container, PlayVideoFragment.newInstance(videoPath))
                .commit();
    }

    @Override
    public void record() {
        currentView = 0;
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O_MR1) {
            //Use camera2
            getFragmentManager().beginTransaction()
                    .replace(R.id.container, Camera2VideoFragment.newInstance(null, 25))
                    .commit();
        } else {
            //Use camera
            getFragmentManager().beginTransaction()
                    .replace(R.id.container, CameraVideoFragment.newInstance(null, 25))
                    .commit();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_VIEW, currentView);
        outState.putString(STATE_PATH, videoPath);
    }
//
//    @Override
//    protected void onRestoreInstanceState(Bundle savedInstanceState) {
//        super.onRestoreInstanceState(savedInstanceState);
//        videoPath = savedInstanceState.getString(STATE_PATH);
//        currentView = savedInstanceState.getInt(STATE_VIEW);
//    }

    @Override
    public void use(String videoPath) {
        Intent intent = new Intent();
        intent.putExtra(EXTRA_SAVE_PATH, videoPath);
        setResult(Activity.RESULT_OK, intent);
        finish();
    }
}