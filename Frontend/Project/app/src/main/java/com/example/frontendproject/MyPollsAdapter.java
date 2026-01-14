package com.example.frontendproject;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class MyPollsAdapter extends RecyclerView.Adapter<MyPollsAdapter.Holder> {

    interface OnPollLongClickListener {
        void onPollLongPressed();
    }

    private final List<Poll> list;
    private final OnPollLongClickListener listener;
    private final List<Integer> selectedPositions = new ArrayList<>();

    public MyPollsAdapter(List<Poll> list, OnPollLongClickListener listener) {
        this.list = list;
        this.listener = listener;
    }

    @Override
    public Holder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_poll_mine, parent, false);
        return new Holder(v);
    }

    @Override
    public void onBindViewHolder(Holder h, int pos) {
        Poll p = list.get(pos);
        h.txtTitle.setText(p.title);
        h.txtType.setText(p.type);

        h.itemView.setBackgroundColor(
                selectedPositions.contains(pos) ?
                        0xFFE0E0E0 :
                        0xFFFFFFFF
        );

        h.itemView.setOnLongClickListener(v -> {
            toggleSelect(pos);
            listener.onPollLongPressed();
            return true;
        });

        h.itemView.setOnClickListener(v -> {
            if (!selectedPositions.isEmpty()) {
                toggleSelect(pos);
            }
        });
    }

    private void toggleSelect(int pos) {
        if (selectedPositions.contains(pos)) selectedPositions.remove((Integer)pos);
        else selectedPositions.add(pos);
        notifyItemChanged(pos);
    }

    public void deleteSelectedPolls() {
        List<Poll> toRemove = new ArrayList<>();
        for (int pos : selectedPositions) toRemove.add(list.get(pos));
        list.removeAll(toRemove);
        selectedPositions.clear();
        notifyDataSetChanged();
    }

    public void clearSelection() {
        selectedPositions.clear();
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() { return list.size(); }

    static class Holder extends RecyclerView.ViewHolder {
        TextView txtTitle, txtType;
        Holder(View v) {
            super(v);
            txtTitle = v.findViewById(R.id.txtPollTitleMine);
            txtType = v.findViewById(R.id.txtPollTypeMine);
        }
    }
}

