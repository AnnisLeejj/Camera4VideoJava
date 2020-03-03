package com.example.android.camera2video.part;

import android.app.Fragment;
import android.content.Context;
import android.content.res.Configuration;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.support.annotation.Nullable;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.VideoView;

import com.example.android.camera2video.R;
import com.example.android.camera2video.listener.VideoRecorderListener;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class CameraVideoFragment extends Fragment implements SurfaceHolder.Callback, MediaRecorder.OnInfoListener, MediaRecorder.OnErrorListener {
    private static final int HANDLER_UPDATE = 5;

    /**
     * 值传递 视频保存路径
     */
    public static final String EXTRA_SAVE_PATH = "EXTRA_SAVE_PATH";
    /***
     * 值传递 视频最长时间 (单位秒)
     */
    public static final String EXTRA_MAX_LENGTH = "EXTRA_MAX_LENGTH";

    public static final int DEFAULT_VIDEO_MAX_LENGTH = 60;
    public static final int DEFAULT_VIDEO_FRAME_RATE = 15;

    private static final int SENSOR_ORIENTATION_DEFAULT_DEGREES = 90;
    private static final int SENSOR_ORIENTATION_INVERSE_DEGREES = 270;
    private static final SparseIntArray DEFAULT_ORIENTATIONS = new SparseIntArray();
    private static final SparseIntArray INVERSE_ORIENTATIONS = new SparseIntArray();

    VideoRecorderListener callBack;
    String mNextVideoAbsolutePath;
    int mVideoMaxLength;
    int frontCamera = 0;
    private Integer mSensorOrientation;

    public static CameraVideoFragment newInstance(String savePath, int maxLength) {
        CameraVideoFragment videoFragment = new CameraVideoFragment();
        Bundle args = new Bundle();
        args.putString(EXTRA_SAVE_PATH, savePath);
        args.putInt(EXTRA_MAX_LENGTH, maxLength);
        videoFragment.setArguments(args);
        return videoFragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof VideoRecorderListener) {
            callBack = (VideoRecorderListener) context;
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle bundle = getArguments();
        mNextVideoAbsolutePath = bundle.getString(EXTRA_SAVE_PATH);
        mVideoMaxLength = bundle.getInt(EXTRA_MAX_LENGTH, DEFAULT_VIDEO_MAX_LENGTH);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_camera_video, container, false);
    }

    private Camera mCamera;

    TextView tvTime;
    Button btVideoControl;
    VideoView mVideoView;
    SurfaceHolder mSurfaceHolder;

    int mVideoFrameRate = 0;
    private MediaRecorder mediaRecorder;// 录制视频的类
    /**
     * Whether the app is recording video now
     */
    private boolean mIsRecordingVideo;

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        tvTime = view.findViewById(R.id.tvTime);
        btVideoControl = view.findViewById(R.id.btVideoControl);
        mVideoView = view.findViewById(R.id.vv);
        btVideoControl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mIsRecordingVideo) {
                    stopRecordingVideo();
                } else {
                    startRecordingVideo();
                }
            }
        });

        mSurfaceHolder = mVideoView.getHolder();
        mSurfaceHolder.addCallback(this);
        mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        PowerManager pm = (PowerManager) getActivity().getSystemService(Context.POWER_SERVICE);
//        mWakeLock = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, CLASS_LABEL);
//        mWakeLock.acquire();

//        frontCamera = getIntent().getIntExtra("cameraposition", 0);
        mNextVideoAbsolutePath = getArguments().getString(EXTRA_SAVE_PATH);
    }

    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            int time = msg.arg1;

            int s = time % 60;
            int m = time / 60;

            StringBuffer buffer = new StringBuffer();

            if (m < 10) {
                buffer.append("0").append(m);
            } else {
                buffer.append(m);
            }
            buffer.append(":");
            if (s < 10) {
                buffer.append("0").append(s);
            } else {
                buffer.append(s);
            }
            tvTime.setText(buffer.toString());
        }
    };

    private void startRecordingVideo() {
        if (!startRecording())
            return;
        // 重置其他
//         progressBar.setProgress(0);

        /**
         * 开始计时,并做标记,设置图标
         */
        btVideoControl.setBackgroundResource(R.drawable.ic_stop);
        mIsRecordingVideo = true;
        startTimer();
        if(callBack!=null){
            callBack.recording(0);
        }
    }

    private void stopRecordingVideo() {
        stopRecording();
        btVideoControl.setBackgroundResource(R.drawable.ic_start);
        mIsRecordingVideo = false;
        stopTimer();
        if (callBack != null) {
            callBack.compare(mNextVideoAbsolutePath);
        }

    }

    CountDownTimer downTimer;

    /**
     * 录像开始,计时器开始
     */
    private void startTimer() {
        tvTime.setVisibility(View.VISIBLE);
        downTimer = new CountDownTimer(mVideoMaxLength * 1_000, 1_000) {
            @Override
            public void onTick(long millisUntilFinished) {

                long time = mVideoMaxLength * 1_000 - millisUntilFinished;
                final int timeSecond = (int) (time / 1000);

                Message msg = new Message();
                msg.arg1 = timeSecond;
                handler.sendMessage(msg);

            }

            @Override
            public void onFinish() {
                btVideoControl.callOnClick();
                tvTime.setVisibility(View.INVISIBLE);
                tvTime.setText("00:00");
            }
        };
        downTimer.start();
    }


    /**
     * 录像结束,计时器结束
     */
    private void stopTimer() {
        tvTime.setText("00:00");
        tvTime.setVisibility(View.INVISIBLE);
        if (downTimer != null) {
            downTimer.cancel();
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (mCamera == null) {
            if (!initCamera()) {
                return;
            }
        }
        try {
            mCamera.setPreviewDisplay(mSurfaceHolder);
            mCamera.startPreview();
            handleSurfaceChanged();
        } catch (Exception e1) {
            e1.printStackTrace();
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        // 将holder，这个holder为开始在onCreate里面取得的holder，将它赋给surfaceHolder
        mSurfaceHolder = holder;
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }

    boolean isFocus;
    int previewWidth, previewHeight;

    private boolean initCamera() {
        try {
            if (frontCamera == 0) {
                mCamera = Camera.open(Camera.CameraInfo.CAMERA_FACING_BACK);
            } else {
                mCamera = Camera.open(Camera.CameraInfo.CAMERA_FACING_FRONT);
            }
            mCamera.lock();
            mSurfaceHolder = mVideoView.getHolder();
            mSurfaceHolder.addCallback(this);
            mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

            if (isFocus) {
                mCamera.autoFocus(null);
            }
            CameraManager manager = (CameraManager) getActivity().getSystemService(Context.CAMERA_SERVICE);
            String cameraId = manager.getCameraIdList()[frontCamera];
            // Choose the sizes for camera preview and video recording
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
            StreamConfigurationMap map = characteristics
                    .get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            mSensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);


            int rotation = getActivity().getWindowManager().getDefaultDisplay().getRotation();
            switch (rotation) {
                case 0:
                    mCamera.setDisplayOrientation(SENSOR_ORIENTATION_DEFAULT_DEGREES);
                    break;
                case 1:
                    mCamera.setDisplayOrientation(0);
                    break;
                case 3:
                    mCamera.setDisplayOrientation(180);
                    break;
            }
        } catch (RuntimeException ex) {
            return false;
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        return true;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        initCamera();
    }

    private boolean initRecorder() {
        if (mCamera == null) {
            if (!initCamera()) {
//                ToastUtil.showShortToast(this, "init failed");
                return false;
            }
        }
        mVideoView.setVisibility(View.VISIBLE);
        // TODO init button
        mCamera.stopPreview();
        mediaRecorder = new MediaRecorder();
        mCamera.unlock();
        mediaRecorder.setCamera(mCamera);
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        // 设置录制视频源为Camera（相机）
        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);

        // 设置录制完成后视频的封装格式THREE_GPP为3gp.MPEG_4为mp4
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        // 设置录制的视频编码h263 h264
        mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        // 设置视频录制的分辨率。必须放在设置编码和格式的后面，否则报错
        mediaRecorder.setVideoSize(previewWidth, previewHeight);
        // 设置视频的比特率
        mediaRecorder.setVideoEncodingBitRate(1 * 1024 * 1024);
        // // 设置录制的视频帧率。必须放在设置编码和格式的后面，否则报错
        if (mVideoFrameRate != -1) {
            mediaRecorder.setVideoFrameRate(mVideoFrameRate);
        }
        // 设置视频文件输出的路径
        if (mNextVideoAbsolutePath == null || mNextVideoAbsolutePath.isEmpty()) {
            mNextVideoAbsolutePath = getVideoFilePath(getActivity());
        }
        mediaRecorder.setOutputFile(mNextVideoAbsolutePath);
//        mediaRecorder.setMaxDuration(mVideoMaxLength);
        mediaRecorder.setPreviewDisplay(mSurfaceHolder.getSurface());

        int rotation = getActivity().getWindowManager().getDefaultDisplay().getRotation();
//        switch (mSensorOrientation) {
//            case SENSOR_ORIENTATION_DEFAULT_DEGREES:
//                mediaRecorder.setOrientationHint(DEFAULT_ORIENTATIONS.get(rotation));
//                break;
//            case SENSOR_ORIENTATION_INVERSE_DEGREES:
//                mediaRecorder.setOrientationHint(INVERSE_ORIENTATIONS.get(rotation));
//                break;
//        }
        switch (rotation) {
            case 0:
                mediaRecorder.setOrientationHint(SENSOR_ORIENTATION_DEFAULT_DEGREES);
                break;
            case 1:
                mediaRecorder.setOrientationHint(0);
                break;
            case 3:
                mediaRecorder.setOrientationHint(180);
                break;
        }
        try {
            mediaRecorder.prepare();
        } catch (IllegalStateException e) {
            e.printStackTrace();
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;

    }

    private void handleSurfaceChanged() {
        if (mCamera == null) {
            getActivity().finish();
            return;
        }
        setCameraParams();
        boolean hasSupportRate = false;
        List<Integer> supportedPreviewFrameRates = mCamera.getParameters()
                .getSupportedPreviewFrameRates();
        if (supportedPreviewFrameRates != null
                && supportedPreviewFrameRates.size() > 0) {
            Collections.sort(supportedPreviewFrameRates);
            for (int i = 0; i < supportedPreviewFrameRates.size(); i++) {
                int supportRate = supportedPreviewFrameRates.get(i);
                if (supportRate == 15) {
                    hasSupportRate = true;
                }
            }
            if (hasSupportRate) {
                mVideoFrameRate = 15;
            } else {
                mVideoFrameRate = supportedPreviewFrameRates.get(0);
            }
        }
        // 获取摄像头的所有支持的分辨率
        List<Camera.Size> resolutionList = getResolutionList(mCamera);
        if (resolutionList != null && resolutionList.size() > 0) {
            Collections.sort(resolutionList, new ResolutionComparator());
            Camera.Size previewSize = null;
            boolean hasSize = false;
            // 如果摄像头支持640*480，那么强制设为640*480
            for (int i = 0; i < resolutionList.size(); i++) {
                Camera.Size size = resolutionList.get(i);
                if (size != null && size.width == 640 && size.height == 480) {
                    previewSize = size;
                    previewWidth = previewSize.width;
                    previewHeight = previewSize.height;
                    hasSize = true;
                    break;
                }
            }
            // 如果不支持设为中间的那个
            if (!hasSize) {
                int mediumResolution = resolutionList.size() / 2;
                if (mediumResolution >= resolutionList.size())
                    mediumResolution = resolutionList.size() - 1;
                previewSize = resolutionList.get(mediumResolution);
                previewWidth = previewSize.width;
                previewHeight = previewSize.height;
            }

        }
    }

    public void setCameraParams() {
        if (mCamera != null) {
            Camera.Parameters params = mCamera.getParameters();
            List<String> list = params.getSupportedFocusModes();
            if (list.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
                isFocus = true;
                params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
            }
            params.set("orientation", "portrait");
            mCamera.setParameters(params);
        }
    }

    private void releaseRecorder() {
        if (mediaRecorder != null) {
            mediaRecorder.release();
            mediaRecorder = null;
        }
    }

    public boolean startRecording() {
        if (mediaRecorder == null) {
            if (!initRecorder())
                return false;
        }
        mediaRecorder.setOnInfoListener(this);
        mediaRecorder.setOnErrorListener(this);
        mediaRecorder.start();
        return true;
    }

    protected void releaseCamera() {
        try {
            if (mCamera != null) {
                mCamera.stopPreview();
                mCamera.release();
                mCamera = null;
            }
        } catch (Exception e) {
        }
    }

    public void stopRecording() {
        stopTimer();
        if (mediaRecorder != null) {
            mediaRecorder.setOnErrorListener(null);
            mediaRecorder.setOnInfoListener(null);
            try {
                mediaRecorder.stop();
            } catch (IllegalStateException e) {
                Log.e("recorder", "stopRecording error:" + e.getMessage());
            }
        }
        releaseRecorder();

        if (mCamera != null) {
            mCamera.stopPreview();
            releaseCamera();
        }

        mIsRecordingVideo = false;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        releaseCamera();
    }

    /**
     * 设备支持的分辨率
     *
     * @param camera
     * @return
     */
    public static List<Camera.Size> getResolutionList(Camera camera) {
        Camera.Parameters parameters = camera.getParameters();
        List<Camera.Size> previewSizes = parameters.getSupportedPreviewSizes();
        return previewSizes;
    }

    @Override
    public void onInfo(MediaRecorder mediaRecorder, int what, int extra) {
        if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED) {
//            LogUtil.v("max duration reached");

            stopRecording();
            if (mNextVideoAbsolutePath == null) {
                return;
            }
            //录制完成
        }
    }

    @Override
    public void onError(MediaRecorder mediaRecorder, int i, int i1) {
        stopRecording();
//        Toast.makeText(this,
//                "Recording error has occurred. Stopping the recording",
//                Toast.LENGTH_SHORT).show();
    }

    private String getVideoFilePath(Context context) {
        final File dir = context.getExternalFilesDir(null);
        File file = new File(dir, "video");
        if (!file.exists()) {
            boolean mkdir = file.mkdir();
            if (mkdir)
                return (dir == null ? "" : (dir.getAbsolutePath() + "/video/"))
                        + System.currentTimeMillis() + ".mp4";
        }
        return (dir == null ? "" : (dir.getAbsolutePath() + "/video/"))
                + System.currentTimeMillis() + ".mp4";
    }

    public static class ResolutionComparator implements Comparator<Camera.Size> {
        @Override
        public int compare(Camera.Size lhs, Camera.Size rhs) {
            if (lhs.height != rhs.height)
                return lhs.height - rhs.height;
            else
                return lhs.width - rhs.width;
        }
    }
}