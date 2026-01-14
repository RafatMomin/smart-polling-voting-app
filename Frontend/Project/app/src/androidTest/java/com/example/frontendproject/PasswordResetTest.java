package com.example.frontendproject;

import android.content.Intent;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

@RunWith(AndroidJUnit4.class)
public class PasswordResetTest {

    @Test
    public void passwordReset_launch_showsFieldsAndButton() {
        ActivityScenario<PasswordReset> scenario =
                ActivityScenario.launch(PasswordReset.class);

        onView(withId(R.id.editToken)).check(matches(isDisplayed()));
        onView(withId(R.id.editNewPassword)).check(matches(isDisplayed()));
        onView(withId(R.id.editConfirmPassword)).check(matches(isDisplayed()));
        onView(withId(R.id.resetPasswordButton)).check(matches(isDisplayed()));

        scenario.close();
    }

    @Test
    public void passwordReset_emptyToken_showsToast() {
        ActivityScenario<PasswordReset> scenario =
                ActivityScenario.launch(PasswordReset.class);

        onView(withId(R.id.editNewPassword)).perform(typeText("abc123"), closeSoftKeyboard());
        onView(withId(R.id.editConfirmPassword)).perform(typeText("abc123"), closeSoftKeyboard());
        onView(withId(R.id.resetPasswordButton)).perform(click());

        scenario.close();
    }

    @Test
    public void passwordReset_passwordsDoNotMatch_showsToast() {
        ActivityScenario<PasswordReset> scenario =
                ActivityScenario.launch(PasswordReset.class);

        onView(withId(R.id.editToken)).perform(typeText("tok123"), closeSoftKeyboard());
        onView(withId(R.id.editNewPassword)).perform(typeText("abc123"), closeSoftKeyboard());
        onView(withId(R.id.editConfirmPassword)).perform(typeText("xyz789"), closeSoftKeyboard());
        onView(withId(R.id.resetPasswordButton)).perform(click());

        scenario.close();
    }

    @Test
    public void passwordReset_fillAllFields_andClickReset() {
        Intent intent = new Intent(
                ApplicationProvider.getApplicationContext(),
                PasswordReset.class
        );
        intent.putExtra("resetToken", "tok123");

        ActivityScenario<PasswordReset> scenario =
                ActivityScenario.launch(intent);

        onView(withId(R.id.editNewPassword)).perform(typeText("abc123"), closeSoftKeyboard());
        onView(withId(R.id.editConfirmPassword)).perform(typeText("abc123"), closeSoftKeyboard());
        onView(withId(R.id.resetPasswordButton)).perform(click());

        scenario.close();
    }
}
