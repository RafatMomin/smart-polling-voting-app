package com.example.frontendproject;

import android.view.LayoutInflater;
import android.widget.TextView;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.recyclerview.widget.RecyclerView;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;

import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class ChatAdapterTest {

    @Test
    public void chatMessage_getters_work() {
        ChatMessage userMsg = new ChatMessage(ChatMessage.ROLE_USER, "Hello");
        ChatMessage botMsg = new ChatMessage(ChatMessage.ROLE_BOT, "Hi");

        assertEquals(ChatMessage.ROLE_USER, userMsg.getRole());
        assertEquals("Hello", userMsg.getText());
        assertEquals(ChatMessage.ROLE_BOT, botMsg.getRole());
        assertEquals("Hi", botMsg.getText());
    }

    @Test
    public void adapter_addUserAndBotMessages_increasesItemCount() {
        ChatAdapter adapter = new ChatAdapter();
        assertEquals(0, adapter.getItemCount());

        adapter.addUserMessage("User says hi");
        adapter.addBotMessage("Bot replies");
        assertEquals(2, adapter.getItemCount());

        assertEquals(ChatMessage.ROLE_USER, adapter.getItems().get(0).getRole());
        assertEquals(ChatMessage.ROLE_BOT, adapter.getItems().get(1).getRole());
    }

    @Test
    public void adapter_addTypingIndicator_andUpdateLastBotMessage() {
        ChatAdapter adapter = new ChatAdapter();
        adapter.addTypingIndicator();
        assertEquals(1, adapter.getItemCount());
        assertEquals("…", adapter.getItems().get(0).getText());

        adapter.updateLastBotMessage("Final reply");
        assertEquals(1, adapter.getItemCount());
        assertEquals("Final reply", adapter.getItems().get(0).getText());
    }

    @Test
    public void adapter_updateLastBotMessage_addsNewIfNoTypingIndicator() {
        ChatAdapter adapter = new ChatAdapter();
        adapter.addUserMessage("Hello");
        adapter.updateLastBotMessage("Bot answer");
        assertEquals(2, adapter.getItemCount());
        assertEquals("Bot answer", adapter.getItems().get(1).getText());
    }

    @Test
    public void adapter_setMessages_replacesList() {
        ChatAdapter adapter = new ChatAdapter();
        adapter.addUserMessage("First");
        assertEquals(1, adapter.getItemCount());

        adapter.setMessages(Arrays.asList(
                new ChatMessage(ChatMessage.ROLE_USER, "New1"),
                new ChatMessage(ChatMessage.ROLE_BOT, "New2")
        ));
        assertEquals(2, adapter.getItemCount());
        assertEquals("New1", adapter.getItems().get(0).getText());
        assertEquals("New2", adapter.getItems().get(1).getText());
    }

    @Test
    public void adapter_getItemViewType_returnsCorrectType() {
        ChatAdapter adapter = new ChatAdapter();
        adapter.addUserMessage("User");
        adapter.addBotMessage("Bot");

        assertEquals(1, adapter.getItemViewType(0)); // VIEW_USER
        assertEquals(2, adapter.getItemViewType(1)); // VIEW_BOT
    }

    @Test
    public void adapter_onCreateViewHolder_andBind_setsText() {
        ChatAdapter adapter = new ChatAdapter();
        adapter.addUserMessage("User text");
        adapter.addBotMessage("Bot text");

        LayoutInflater inflater = LayoutInflater.from(ApplicationProvider.getApplicationContext());
        RecyclerView.ViewHolder userVH = adapter.onCreateViewHolder(
                new android.widget.FrameLayout(ApplicationProvider.getApplicationContext()), 1);
        RecyclerView.ViewHolder botVH = adapter.onCreateViewHolder(
                new android.widget.FrameLayout(ApplicationProvider.getApplicationContext()), 2);

        adapter.onBindViewHolder(userVH, 0);
        adapter.onBindViewHolder(botVH, 1);

        assertTrue(((TextView)((ChatAdapter.UserVH)userVH).tv).getText().toString().contains("User"));
        assertTrue(((TextView)((ChatAdapter.BotVH)botVH).tv).getText().toString().contains("Bot"));
    }
}
