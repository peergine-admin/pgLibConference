package com.peergine.conference.demo;

import android.os.Bundle;

import me.yokeyword.fragmentation.SupportActivity;

public class MainActivity extends SupportActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if(findFragment(ParamFragment.class) == null){
            loadRootFragment(R.id.fragment,ParamFragment.newInstance());
        }
    }
}
