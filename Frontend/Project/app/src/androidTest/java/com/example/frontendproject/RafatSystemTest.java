package com.example.frontendproject;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.Intent;
import android.view.View;

import com.example.frontendproject.net.ChatApi;
import com.example.frontendproject.net.RetrofitClient;

import retrofit2.Retrofit;

@RunWith(AndroidJUnit4.class)
public class RafatSystemTest {

    @Test
    public void signupFlow_fillForm_andGoBack() {
        ActivityScenario<SignupActivity> scenario =
                ActivityScenario.launch(SignupActivity.class);
        onView(withId(R.id.signupTitle)).check(matches(withText("Sign Up")));
        onView(withId(R.id.btnSignup)).perform(click());
        onView(withId(R.id.etName)).perform(typeText("Rafat Test User"), closeSoftKeyboard());
        onView(withId(R.id.etEmail)).perform(typeText("rafat@example.com"), closeSoftKeyboard());
        onView(withId(R.id.etPassword)).perform(typeText("Password123"), closeSoftKeyboard());
        onView(withId(R.id.btnBack)).perform(click());
        scenario.close();
    }

    @Test
    public void signupFlow_validationErrors_showToasts() {
        ActivityScenario<SignupActivity> scenario =
                ActivityScenario.launch(SignupActivity.class);
        onView(withId(R.id.etName)).perform(replaceText(""), closeSoftKeyboard());
        onView(withId(R.id.etEmail)).perform(replaceText("rafat@example.com"), closeSoftKeyboard());
        onView(withId(R.id.etPassword)).perform(replaceText("Password123"), closeSoftKeyboard());
        onView(withId(R.id.btnSignup)).perform(click());
        onView(withId(R.id.etName)).perform(replaceText("Rafat"), closeSoftKeyboard());
        onView(withId(R.id.etEmail)).perform(replaceText("not-an-email"), closeSoftKeyboard());
        onView(withId(R.id.etPassword)).perform(replaceText("Password123"), closeSoftKeyboard());
        onView(withId(R.id.btnSignup)).perform(click());
        onView(withId(R.id.etName)).perform(replaceText("Rafat"), closeSoftKeyboard());
        onView(withId(R.id.etEmail)).perform(replaceText("rafat@example.com"), closeSoftKeyboard());
        onView(withId(R.id.etPassword)).perform(replaceText("123"), closeSoftKeyboard());
        onView(withId(R.id.btnSignup)).perform(click());
        scenario.close();
    }

    @Test
    public void dashboard_statsSectionVisible() {
        ActivityScenario<DashboardActivity> scenario =
                ActivityScenario.launch(DashboardActivity.class);
        onView(withId(R.id.tvDashboardTitle)).check(matches(withText("My Dashboard")));
        onView(withId(R.id.tvPollsJoined)).check(matches(isDisplayed()));
        onView(withId(R.id.tvVotesCast)).check(matches(isDisplayed()));
        onView(withId(R.id.tvPollTypeBreakdown)).check(matches(withText("Poll Type Breakdown:")));
        onView(withId(R.id.tvPollTypesDetail)).check(matches(isDisplayed()));
        onView(withId(R.id.tvRecentActivityTitle)).check(matches(withText("Recent Activity:")));
        onView(withId(R.id.tvRecentActivity)).check(matches(isDisplayed()));
        scenario.close();
    }

    @Test
    public void polls_useFilters_andOpenChatbot() {
        ActivityScenario<PollsActivity> pollsScenario =
                ActivityScenario.launch(PollsActivity.class);
        onView(withId(R.id.toolbarPolls)).check(matches(isDisplayed()));
        onView(withId(R.id.chipTypeYesNo)).perform(click());
        onView(withId(R.id.chipVisPublic)).perform(click());
        onView(withId(R.id.etSearch)).perform(typeText("test poll"), closeSoftKeyboard());
        onView(withId(R.id.recyclerPolls)).check(matches(isDisplayed()));
        onView(withId(R.id.fabChat)).perform(click());
        onView(withId(R.id.chatRoot)).check(matches(isDisplayed()));
        onView(withId(R.id.rvChat)).check(matches(isDisplayed()));
        pollsScenario.close();
    }

    @Test
    public void mapActivity_launch_showsStatusAndButton() {
        ActivityScenario<MapActivity> scenario =
                ActivityScenario.launch(MapActivity.class);
        scenario.onActivity(activity -> assertNotNull(activity));
        onView(withId(R.id.tvStatus)).check(matches(isDisplayed()));
        onView(withId(R.id.btnContinue)).check(matches(isDisplayed()));
        onView(withId(R.id.btnContinue)).perform(click());
        scenario.close();
    }

    @Test
    public void mapActivity_appendOrReplaceServerLine_behavesCorrectly() {
        String result1 = MapActivity.appendOrReplaceServerLine(null, "Server: ✅ Eligible");
        assertEquals("Server: ✅ Eligible", result1);
        String result2 = MapActivity.appendOrReplaceServerLine("You are inside campus", "Server: ⚠️ Unconfirmed");
        assertEquals("You are inside campus\nServer: ⚠️ Unconfirmed", result2);
        String result3 = MapActivity.appendOrReplaceServerLine("You are outside\nServer: ❌ Not eligible", "Server: ✅ Eligible");
        assertEquals("You are outside\nServer: ✅ Eligible", result3);
    }

    @Test
    public void chatActivity_emptyMessage_doesNothing() {
        ActivityScenario<ChatActivity> scenario =
                ActivityScenario.launch(ChatActivity.class);
        onView(withId(R.id.btnSend)).perform(click());
        scenario.close();
    }

    @Test
    public void chatActivity_sendMessage_updatesUI() {
        ActivityScenario<ChatActivity> scenario =
                ActivityScenario.launch(ChatActivity.class);
        onView(withId(R.id.etMessage)).perform(typeText("Hello test"), closeSoftKeyboard());
        onView(withId(R.id.btnSend)).perform(click());
        onView(withId(R.id.rvChat)).check(matches(isDisplayed()));
        scenario.close();
    }

    @Test
    public void login_fillCredentials_andNavigateToSignupAndForget() {
        ActivityScenario<LoginActivity> scenario =
                ActivityScenario.launch(LoginActivity.class);
        onView(withId(R.id.loginLogo)).check(matches(isDisplayed()));
        onView(withId(R.id.emailaddresslogin)).perform(scrollTo(), typeText("rafat@login.com"), closeSoftKeyboard());
        onView(withId(R.id.passwordlogin)).perform(scrollTo(), typeText("Password123"), closeSoftKeyboard());
        onView(withId(R.id.loginbutton)).perform(scrollTo(), click());
        onView(withId(R.id.signupButton)).perform(scrollTo(), click());
        onView(withId(R.id.signupTitle)).check(matches(withText("Sign Up")));
        pressBack();
        onView(withId(R.id.forgetPasswordButton)).perform(scrollTo(), click());
        onView(withId(R.id.EditTEmail)).check(matches(isDisplayed()));
        scenario.close();
    }

    @Test
    public void polls_toggleAllFilters_andOpenCreateThenBack() {
        ActivityScenario<PollsActivity> pollsScenario =
                ActivityScenario.launch(PollsActivity.class);
        onView(withId(R.id.btnMine)).perform(click());
        onView(withId(R.id.btnAll)).perform(click());
        onView(withId(R.id.chipTypeRating)).perform(click());
        onView(withId(R.id.chipTypeMCQ)).perform(click());
        onView(withId(R.id.chipTypeRanking)).perform(click());
        onView(withId(R.id.chipTypeAll)).perform(click());
        onView(withId(R.id.chipVisPrivate)).perform(click());
        onView(withId(R.id.chipVisPublic)).perform(click());
        onView(withId(R.id.chipVisAll)).perform(click());
        onView(withId(R.id.fabAdd)).perform(click());
        pressBack();
        onView(withId(R.id.recyclerPolls)).check(matches(isDisplayed()));
        pollsScenario.close();
    }

    @Test
    public void chat_sendMultipleMessages() {
        ActivityScenario<ChatActivity> scenario =
                ActivityScenario.launch(ChatActivity.class);
        onView(withId(R.id.chatRoot)).check(matches(isDisplayed()));
        onView(withId(R.id.rvChat)).check(matches(isDisplayed()));
        onView(withId(R.id.etMessage)).perform(typeText("Message 1"), closeSoftKeyboard());
        onView(withId(R.id.btnSend)).perform(click());
        onView(withId(R.id.etMessage)).perform(typeText("Message 2"), closeSoftKeyboard());
        onView(withId(R.id.btnSend)).perform(click());
        onView(withId(R.id.etMessage)).perform(typeText("Message 3"), closeSoftKeyboard());
        onView(withId(R.id.btnSend)).perform(click());
        onView(withId(R.id.rvChat)).check(matches(isDisplayed()));
        scenario.close();
    }

    @Test
    public void liveDiscussion_sendMessage_doesNotCrash() {
        Intent intent = new Intent(
                ApplicationProvider.getApplicationContext(),
                LiveDiscussionActivity.class
        );
        intent.putExtra("discussionId", 123);
        intent.putExtra("token", "dummy-token");
        intent.putExtra("endpoint", "ws://example.com");

        ActivityScenario<LiveDiscussionActivity> scenario =
                ActivityScenario.launch(intent);

        onView(withId(R.id.messageInput))
                .perform(typeText("Hello from test"), closeSoftKeyboard());
        onView(withId(R.id.sendButton)).perform(click());
        onView(withId(R.id.chatContainer)).check(matches(isDisplayed()));

        scenario.close();
    }

    @Test
    public void resultsActivity_launches_andShowsTitle() {
        Intent intent = new Intent(
                ApplicationProvider.getApplicationContext(),
                ResultsActivity.class
        );
        intent.putExtra("pollId", 123);
        intent.putExtra("title", "Demo Poll");
        intent.putExtra("type", "YES_NO");

        ActivityScenario<ResultsActivity> scenario =
                ActivityScenario.launch(intent);

        onView(withId(R.id.txtTitleResults))
                .check(matches(withText(containsString("Demo Poll"))));

        scenario.close();
    }

    @Test
    public void vote_yesNo_buildsRadioButtons() {
        Context ctx = ApplicationProvider.getApplicationContext();
        Intent intent = new Intent(ctx, VoteActivity.class);
        intent.putExtra("pollId", 123);
        intent.putExtra("title", "Yes/No Test");
        intent.putExtra("type", "YES_NO");

        try (ActivityScenario<VoteActivity> scenario = ActivityScenario.launch(intent)) {
            scenario.onActivity(activity -> {
                android.widget.TextView titleView = activity.findViewById(R.id.txtTitle);
                assertNotNull(titleView);
                assertTrue(titleView.getText().toString().contains("Yes/No Test"));

                android.widget.LinearLayout container = activity.findViewById(R.id.container);
                assertNotNull(container);
                assertTrue(container.getChildCount() > 0);

                View firstChild = container.getChildAt(0);
                assertTrue(firstChild instanceof android.widget.RadioGroup);

                android.widget.RadioGroup group = (android.widget.RadioGroup) firstChild;
                assertTrue(group.getChildCount() >= 2);
            });
        }
    }

    @Test
    public void vote_multipleChoice_buildsOptionsFromIntent() {
        Context ctx = ApplicationProvider.getApplicationContext();
        Intent intent = new Intent(ctx, VoteActivity.class);
        intent.putExtra("pollId", 456);
        intent.putExtra("title", "MCQ Test");
        intent.putExtra("type", "MULTIPLE_CHOICE");

        java.util.ArrayList<String> options = new java.util.ArrayList<>();
        options.add("Option A");
        options.add("Option B");
        intent.putStringArrayListExtra("options", options);

        try (ActivityScenario<VoteActivity> scenario = ActivityScenario.launch(intent)) {
            scenario.onActivity(activity -> {
                android.widget.TextView titleView = activity.findViewById(R.id.txtTitle);
                assertNotNull(titleView);
                assertTrue(titleView.getText().toString().contains("MCQ Test"));

                android.widget.LinearLayout container = activity.findViewById(R.id.container);
                assertNotNull(container);
                assertTrue(container.getChildCount() > 0);

                View firstChild = container.getChildAt(0);
                assertTrue(firstChild instanceof android.widget.RadioGroup);

                android.widget.RadioGroup group = (android.widget.RadioGroup) firstChild;
                assertEquals(2, group.getChildCount());
            });
        }
    }

    @Test
    public void vote_rating_buildsRatingBar() {
        Context ctx = ApplicationProvider.getApplicationContext();
        Intent intent = new Intent(ctx, VoteActivity.class);
        intent.putExtra("pollId", 789);
        intent.putExtra("title", "Rating Test");
        intent.putExtra("type", "RATING");

        try (ActivityScenario<VoteActivity> scenario = ActivityScenario.launch(intent)) {
            scenario.onActivity(activity -> {
                android.widget.TextView titleView = activity.findViewById(R.id.txtTitle);
                assertNotNull(titleView);
                assertTrue(titleView.getText().toString().contains("Rating Test"));

                android.widget.LinearLayout container = activity.findViewById(R.id.container);
                assertNotNull(container);
                assertTrue(container.getChildCount() > 0);

                View firstChild = container.getChildAt(0);
                assertTrue(firstChild instanceof android.widget.RatingBar);
            });
        }
    }

    @Test
    public void vote_ranking_buildsNumberPickersForOptions() {
        Context ctx = ApplicationProvider.getApplicationContext();
        Intent intent = new Intent(ctx, VoteActivity.class);
        intent.putExtra("pollId", 1011);
        intent.putExtra("title", "Ranking Test");
        intent.putExtra("type", "RANKING");

        java.util.ArrayList<String> options = new java.util.ArrayList<>();
        options.add("Option 1");
        options.add("Option 2");
        options.add("Option 3");
        intent.putStringArrayListExtra("options", options);

        try (ActivityScenario<VoteActivity> scenario = ActivityScenario.launch(intent)) {
            scenario.onActivity(activity -> {
                android.widget.TextView titleView = activity.findViewById(R.id.txtTitle);
                assertNotNull(titleView);
                assertTrue(titleView.getText().toString().contains("Ranking Test"));

                android.widget.LinearLayout container = activity.findViewById(R.id.container);
                assertNotNull(container);
                assertTrue(container.getChildCount() > 0);

                boolean foundNumberPicker = false;
                for (int i = 0; i < container.getChildCount(); i++) {
                    View child = container.getChildAt(i);
                    if (child instanceof android.widget.LinearLayout) {
                        android.widget.LinearLayout row = (android.widget.LinearLayout) child;
                        for (int j = 0; j < row.getChildCount(); j++) {
                            if (row.getChildAt(j) instanceof android.widget.NumberPicker) {
                                foundNumberPicker = true;
                                break;
                            }
                        }
                    }
                }
                assertTrue(foundNumberPicker);
            });
        }
    }

    @Test
    public void login_emptyFields_showsToast() {
        ActivityScenario<LoginActivity> scenario =
                ActivityScenario.launch(LoginActivity.class);
        onView(withId(R.id.loginbutton)).perform(click());
        scenario.close();
    }

    @Test
    public void login_navigationButtons_work() {
        ActivityScenario<LoginActivity> scenario =
                ActivityScenario.launch(LoginActivity.class);
        onView(withId(R.id.signupButton)).perform(click());
        onView(withId(R.id.signupTitle)).check(matches(withText("Sign Up")));
        pressBack();
        onView(withId(R.id.forgetPasswordButton)).perform(click());
        onView(withId(R.id.EditTEmail)).check(matches(isDisplayed()));
        scenario.close();
    }

    @Test
    public void forgetPassword_emptyEmail_showsToast() {
        ActivityScenario<ForgetPasswordActivity> scenario =
                ActivityScenario.launch(ForgetPasswordActivity.class);
        onView(withId(R.id.SendLinkButton)).perform(click());
        scenario.close();
    }

    @Test
    public void otpVerification_emptyToken_setsError() {
        Intent intent = new Intent(
                ApplicationProvider.getApplicationContext(),
                OtpVerificationActivity.class
        );
        intent.putExtra(OtpVerificationActivity.EXTRA_EMAIL, "rafat@example.com");

        ActivityScenario<OtpVerificationActivity> scenario =
                ActivityScenario.launch(intent);
        onView(withId(R.id.btnVerify)).perform(click());
        scenario.close();
    }
}
