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
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

@RunWith(AndroidJUnit4.class)
public class DashboardActivityTest {

    private Intent buildIntent() {
        Context ctx = ApplicationProvider.getApplicationContext();
        Intent intent = new Intent(ctx, DashboardActivity.class);
        return intent;
    }

    @Before
    public void seedSession() {
        Context ctx = ApplicationProvider.getApplicationContext();
        SessionManager session = new SessionManager(ctx);
        // Seed a dummy token so DashboardActivity doesn’t exit immediately
        session.saveLogin("dummy-token", "rafat@example.com");
    }

    @Test
    public void dashboardActivity_launch_showsBasicUI() {
        ActivityScenario<DashboardActivity> scenario =
                ActivityScenario.launch(buildIntent());

        onView(withId(R.id.tvDashboardTitle)).check(matches(withText("My Dashboard")));
        onView(withId(R.id.tvPollsJoined)).perform(scrollTo()).check(matches(isDisplayed()));
        onView(withId(R.id.tvVotesCast)).perform(scrollTo()).check(matches(isDisplayed()));
        onView(withId(R.id.tvPollTypeBreakdown)).perform(scrollTo()).check(matches(isDisplayed()));
        onView(withId(R.id.tvPollTypesDetail)).perform(scrollTo()).check(matches(isDisplayed()));
        onView(withId(R.id.tvRecentActivityTitle)).perform(scrollTo()).check(matches(isDisplayed()));
        onView(withId(R.id.tvRecentActivity)).perform(scrollTo()).check(matches(isDisplayed()));

        scenario.close();
    }

    @Test
    public void dashboardActivity_recentActivity_showsDefaultText() {
        ActivityScenario<DashboardActivity> scenario =
                ActivityScenario.launch(buildIntent());

        // By default, recent activity text is "No recent activity yet."
        onView(withId(R.id.tvRecentActivity)).check(matches(withText("No recent activity yet.")));

        scenario.close();
    }
}
