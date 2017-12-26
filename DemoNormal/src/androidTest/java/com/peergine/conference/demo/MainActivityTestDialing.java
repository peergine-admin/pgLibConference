package com.peergine.conference.demo;

import android.support.test.espresso.ViewInteraction;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.test.InstrumentationTestCase;
import android.test.suitebuilder.annotation.LargeTest;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.closeSoftKeyboard;
import static android.support.test.espresso.action.ViewActions.typeText;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.RootMatchers.withDecorView;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static java.lang.Thread.sleep;
import static org.hamcrest.Matchers.not;

/**
 * Copyright (C) 2014-2017, Peergine, All rights reserved.
 * www.peergine.com, www.pptun.com
 * com.peergine.conference.demo
 *
 * @author ctkj
 * @date 2017/12/25.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class MainActivityTestDialing extends InstrumentationTestCase {


    private static final String STRING_TO_BE_TYPED = "Peter";
    private MainActivity mainActivity;
    @Rule
    public ActivityTestRule<MainActivity> mActivityRule = new ActivityTestRule<>(
            MainActivity.class);

    @Before
    public void switchToLogin () {
        mainActivity = mActivityRule.getActivity();
    }

    private void Delay(int millis) {
        try {
            sleep(millis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void sayHello() throws Throwable {
        //获取 Fragment 中的 text
        ViewInteraction fragmentText = onView(withId(R.id.editText_user));
        //验证 text 不存在
        fragmentText.check(matches(isDisplayed()));


        onView(withId(R.id.editText_user)).perform(typeText("hwq2016002"), closeSoftKeyboard()); //line 1
        Delay(2000);
        onView(withId(R.id.btnInitDefault)).perform(click());
        onView(withText("OK")).inRoot(withDecorView(not(mainActivity.getWindow().getDecorView()))).perform(click());

        onView(withText("已经登录")).inRoot(withDecorView(not(mainActivity.getWindow().getDecorView()))).check(matches(isDisplayed()));

        onView(withId(R.id.editText_chair)).perform(typeText("hwq2016001"), closeSoftKeyboard());
        onView(withId(R.id.btn_Start)).perform(click());
        while (true) {

//            onView(withId(R.id.btn_init)).perform(click());
//
//            Delay(10000);
//            onView(withId(R.id.btn_AudioTClick)).perform(click());
//            onView(withId(R.id.btn_AudioRecordStart)).perform(click());
//            Delay(20000);
//            onView(withId(R.id.btn_AudioRecordStop)).perform(click());
//            onView(withId(R.id.btn_AudioTClickStop)).perform(click());
//            Delay(5000);
//
//            onView(withId(R.id.btn_VideoTClick)).perform(click());
//            onView(withId(R.id.btn_VideoRecordStart)).perform(click());
//            onView(withId(R.id.btn_FilePutRequest)).perform(click());
//
//            Delay(20000);
//            onView(withId(R.id.btn_VideoRecordStop)).perform(click());
//            onView(withId(R.id.btn_VideoTClickStop)).perform(click());
//
//            Delay(20000);
//
//            onView(withId(R.id.btn_VideoBackStart)).perform(click());
//            onView(withId(R.id.btn_VBRecordStart)).perform(click());
//            Delay(5000);
//
//            onView(withId(R.id.btn_FilePutRequest)).perform(click());
//
//            Delay(20000);
//            onView(withId(R.id.btn_VBRecordStop)).perform(click());
//            onView(withId(R.id.btn_VideoBackStop)).perform(click());
//
//            Delay(5000);
//
//            onView(withId(R.id.btn_Msg)).perform(click());
//
//            Delay(5000);
//
//            onView(withId(R.id.btn_Multicast)).perform(click());
//
//            Delay(5000);
//
//            onView(withId(R.id.btn_Clean)).perform(click());
            Delay(5000);
        }
    }

}