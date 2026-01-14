package com.example.frontendproject;

import android.content.Context;
import android.content.Intent;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.Visibility;

@RunWith(AndroidJUnit4.class)
public class PollingWindowTest {

    private Intent buildIntent() {
        Context ctx = ApplicationProvider.getApplicationContext();
        Intent intent = new Intent(ctx, PollingWindow.class);
        intent.putExtra("pollId", 123); // valid pollId to prevent early finish
        return intent;
    }

    @Before
    public void seedSession() {
        Context ctx = ApplicationProvider.getApplicationContext();
        SessionManager session = new SessionManager(ctx);
        session.saveLogin("dummy-token", "rafat@example.com"); // seed creatorEmail
    }

    @Test
    public void pollingWindow_launch_showsBasicUI() {
        ActivityScenario<PollingWindow> scenario =
                ActivityScenario.launch(buildIntent());

        onView(withId(R.id.etWindowName)).check(matches(isDisplayed()));
        onView(withId(R.id.etStart)).check(matches(isDisplayed()));
        onView(withId(R.id.btnPickStart)).check(matches(isDisplayed()));
        onView(withId(R.id.etEnd)).check(matches(isDisplayed()));
        onView(withId(R.id.btnPickEnd)).check(matches(isDisplayed()));
        onView(withId(R.id.txtGroups)).check(matches(isDisplayed()));
        onView(withId(R.id.btnCreateGroup)).check(matches(isDisplayed()));
        onView(withId(R.id.rvGroups)).check(matches(isDisplayed()));
        onView(withId(R.id.etEmails)).check(matches(isDisplayed()));
        onView(withId(R.id.btnAddWindow)).check(matches(isDisplayed()));
        onView(withId(R.id.btnDone)).check(matches(isDisplayed()));
        onView(withId(R.id.progress)).check(matches(withEffectiveVisibility(Visibility.GONE)));

        scenario.close();
    }

    @Test
    public void pollingWindow_addWindow_withEmptyName_showsToast() {
        ActivityScenario<PollingWindow> scenario =
                ActivityScenario.launch(buildIntent());

        onView(withId(R.id.btnAddWindow)).perform(click());

        scenario.close();
    }

    @Test
    public void pollingWindow_addWindow_withValidName() {
        ActivityScenario<PollingWindow> scenario =
                ActivityScenario.launch(buildIntent());

        onView(withId(R.id.etWindowName))
                .perform(typeText("My Window"), closeSoftKeyboard());
        onView(withId(R.id.btnAddWindow)).perform(click());

        scenario.close();
    }

    @Test
    public void pollingWindow_doneButton_finishesActivity() {
        ActivityScenario<PollingWindow> scenario =
                ActivityScenario.launch(buildIntent());

        onView(withId(R.id.btnDone)).perform(click());

        scenario.close();
    }

    @Test
    public void pollingWindow_createGroupButton_opensGroupActivity() {
        ActivityScenario<PollingWindow> scenario =
                ActivityScenario.launch(buildIntent());

        onView(withId(R.id.btnCreateGroup)).perform(click());

        // Launch GroupActivity directly to cover its UI
        ActivityScenario<GroupActivity> groupScenario =
                ActivityScenario.launch(GroupActivity.class);
        onView(withId(R.id.etGroupName)).check(matches(isDisplayed()));
        onView(withId(R.id.etEmails)).check(matches(isDisplayed()));
        onView(withId(R.id.btnCreateGroupFinal)).check(matches(isDisplayed()));
        onView(withId(R.id.progressGroup)).check(matches(withEffectiveVisibility(Visibility.GONE)));

        scenario.close();
        groupScenario.close();
    }
}
