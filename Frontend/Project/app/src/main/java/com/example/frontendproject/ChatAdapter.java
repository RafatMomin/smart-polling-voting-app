package com.example.frontendproject;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;


public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_USER = 1;
    private static final int VIEW_BOT  = 2;

    private final List<ChatMessage> items = new ArrayList<>();

    public void setMessages(List<ChatMessage> msgs) {
        items.clear();
        items.addAll(msgs);
        notifyDataSetChanged();
    }

    public List<ChatMessage> getItems() {
        return items;
    }

    public void addUserMessage(String text) {
        items.add(new ChatMessage(ChatMessage.ROLE_USER, text));
        notifyItemInserted(items.size() - 1);
    }

    public void addBotMessage(String text) {
        items.add(new ChatMessage(ChatMessage.ROLE_BOT, text));
        notifyItemInserted(items.size() - 1);
    }

    public void addTypingIndicator() {
        items.add(new ChatMessage(ChatMessage.ROLE_BOT, "…"));
        notifyItemInserted(items.size() - 1);
    }

    public void updateLastBotMessage(String text) {
        int last = items.size() - 1;
        if (last >= 0 && items.get(last).getRole() == ChatMessage.ROLE_BOT && "…".equals(items.get(last).getText())) {
            items.set(last, new ChatMessage(ChatMessage.ROLE_BOT, text));
            notifyItemChanged(last);
        } else {
            addBotMessage(text);
        }
    }

    @Override
    public int getItemViewType(int position) {
        return items.get(position).getRole() == ChatMessage.ROLE_USER ? VIEW_USER : VIEW_BOT;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inf = LayoutInflater.from(parent.getContext());
        View v;
        if (viewType == VIEW_USER) {
            v = inf.inflate(R.layout.item_message_user, parent, false);
            return new UserVH(v);
        } else {
            v = inf.inflate(R.layout.item_message_bot, parent, false);
            return new BotVH(v);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        String text = items.get(position).getText();
        if (holder instanceof UserVH) ((UserVH) holder).tv.setText(text);
        else if (holder instanceof BotVH) ((BotVH) holder).tv.setText(text);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class UserVH extends RecyclerView.ViewHolder {
        TextView tv;
        UserVH(@NonNull View itemView) {
            super(itemView);
            tv = itemView.findViewById(R.id.tvText);
        }
    }

    static class BotVH extends RecyclerView.ViewHolder {
        TextView tv;
        BotVH(@NonNull View itemView) {
            super(itemView);
            tv = itemView.findViewById(R.id.tvText);
        }
    }

}
