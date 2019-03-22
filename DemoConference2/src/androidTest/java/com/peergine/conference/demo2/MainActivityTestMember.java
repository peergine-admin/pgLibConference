package com.peergine.conference.demo2;

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
import static android.support.test.espresso.action.ViewActions.clearText;
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
public class MainActivityTestMember extends InstrumentationTestCase {


    private static final String sChairID = "";
    private static final String sSelfID = "";
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


        onView(withId(R.id.editText_user)).perform(clearText(),typeText(sSelfID), closeSoftKeyboard()); //line 1
        Delay(20);
        onView(withId(R.id.btnInitDefault)).perform(click());
        onView(withText("OK")).inRoot(withDecorView(not(mainActivity.getWindow().getDecorView()))).perform(click());

        onView(withText("已经登录")).inRoot(withDecorView(not(mainActivity.getWindow().getDecorView()))).check(matches(isDisplayed()));

        onView(withId(R.id.editText_chair)).perform(clearText(),typeText(sChairID), closeSoftKeyboard());
        int i =0;
        while (i<100) {
            onView(withId(R.id.btn_Start)).perform(click());

            Delay(100);
            onView(withId(R.id.btn_notifysend)).perform(click());
            Delay(100);
            onView(withId(R.id.btn_msg)).perform(click());
            Delay(100);
            onView(withId(R.id.btn_svr_request)).perform(click());
            Delay(100);
            onView(withId(R.id.btn_file_put)).perform(click());
            Delay(1000);
            onView(withId(R.id.btn_file_get)).perform(click());
            Delay(2000);
            onView(withId(R.id.btn_recordstart)).perform(click());
            Delay(300000);
            onView(withId(R.id.btn_recordstop)).perform(click());
            Delay(2000);

            onView(withId(R.id.btn_stop)).perform(click());

            Delay(5000);
        }
    }

}