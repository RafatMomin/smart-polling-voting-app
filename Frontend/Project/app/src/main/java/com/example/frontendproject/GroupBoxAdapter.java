package com.example.frontendproject;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class GroupBoxAdapter extends RecyclerView.Adapter<GroupBoxAdapter.VH> {

    private final List<PollingWindow.GroupItem> data;

    public GroupBoxAdapter(List<PollingWindow.GroupItem> data) {
        this.data = data;
    }

    public void setData(List<PollingWindow.GroupItem> list) {
        data.clear();
        data.addAll(list);
        notifyDataSetChanged();
    }

    public List<Integer> getSelectedIds() {
        List<Integer> ids = new ArrayList<>();
        for (PollingWindow.GroupItem g : data) if (g.checked) ids.add(g.id);
        return ids;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.activity_groupbox, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        PollingWindow.GroupItem g = data.get(position);
        holder.cb.setText(g.name);
        holder.cb.setOnCheckedChangeListener(null);
        holder.cb.setChecked(g.checked);

        holder.cb.setOnCheckedChangeListener((b, ch) -> g.checked = ch);
    }

    @Override
    public int getItemCount() { return data.size(); }

    static class VH extends RecyclerView.ViewHolder {
        CheckBox cb;
        VH(@NonNull View itemView) {
            super(itemView);
            cb = itemView.findViewById(R.id.cbGroup);
        }
    }
}

