package com.example.android.camera2video.part;

import android.app.Fragment;
import android.content.Context;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.VideoView;

import com.example.android.camera2video.R;
import com.example.android.camera2video.listener.DecisionListener;

public class PlayVideoFragment extends Fragment {
    private static final String ARG_PARAM1 = "param1";

    public PlayVideoFragment() {

    }

    public static PlayVideoFragment newInstance(String videoPath) {
        PlayVideoFragment fragment = new PlayVideoFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, videoPath);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            videoPath = getArguments().getString(ARG_PARAM1);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_play_video, container, false);
    }

    private String videoPath;
    DecisionListener listener;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof DecisionListener) {
            listener = (DecisionListener) context;
        }
    }

    VideoView videoView;
    Button btVideoControl;
    ImageView firstImage;

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        videoView = view.findViewById(R.id.vv);
        btVideoControl = view.findViewById(R.id.btVideoControl);
        firstImage = view.findViewById(R.id.iv);

        videoView.setVideoPath(videoPath);
        videoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                btVideoControl.setBackgroundResource(R.drawable.ic_play);
            }
        });

        btVideoControl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (videoView.isPlaying()) {
                    videoView.pause();
                    v.setBackgroundResource(R.drawable.ic_play);
                } else {
                    firstImage.setVisibility(View.GONE);
                    videoView.start();
                    v.setBackgroundResource(R.drawable.ic_pause);
                }
            }
        });
        view.findViewById(R.id.tvRecord).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) {
                    listener.record();
                }
            }
        });

        view.findViewById(R.id.tvUse).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) {
                    listener.use(videoPath);
                }
            }
        });

        showFirst();
    }

    private void showFirst() {
        MediaMetadataRetriever metadataRetriever = new MediaMetadataRetriever();

        metadataRetriever.setDataSource(videoPath);
        Bitmap frameAtTime = metadataRetriever.getFrameAtTime();
        firstImage.setImageBitmap(frameAtTime);
    }
}