package com.peergine.conference.demo2;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.peergine.conference.democonference2.R;

import me.yokeyword.fragmentation.SupportFragment;

/**
 * Copyright (C) 2014-2017, Peergine, All rights reserved.
 * www.peergine.com, www.pptun.com
 * com.peergine.conference.demo
 *
 * @author ctkj
 * @date 2018/3/19.
 */

public class FullScreenFragment extends SupportFragment {

    public SurfaceView videoview =null;

    public int iWndID = 0;

    private LinearLayout layoutVideo = null;

    public static FullScreenFragment newInstance(SurfaceView view,int iWndID) {

        FullScreenFragment fragment = new FullScreenFragment();

        fragment.videoview=view;

        fragment.iWndID = iWndID;

        Bundle args = new Bundle();

        fragment.setArguments(args);

        return fragment;
    }

    void initView(View view) {
        layoutVideo = view.findViewById(R.id.fs_video);

        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Bundle bundle = new Bundle();
                bundle.putInt("WndID",iWndID);
                setFragmentResult(RESULT_OK,bundle);
                pop();
            }
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_fullscreen, container, false);
        initView(view);

        if(videoview!=null) {
            layoutVideo.addView(videoview);
        }else {
            Toast.makeText(getContext(),"videoview == NULL .",Toast.LENGTH_SHORT).show();
        }

        return view;
    }


    @Override
    public void onDestroyView() {

        layoutVideo.removeAllViews();

        super.onDestroyView();

    }
}
