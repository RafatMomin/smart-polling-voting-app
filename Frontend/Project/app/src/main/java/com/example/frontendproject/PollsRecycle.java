package com.example.frontendproject;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;

import java.util.List;

public class PollsRecycle extends RecyclerView.Adapter<PollsRecycle.VH> {

    public interface OnClick { void onClick(Poll p); }

    private static final int VT_YESNO = 0;
    private static final int VT_RATING = 1;
    private static final int VT_MULTIPLE = 2;
    private static final int VT_RANK = 3;

    private final List<Poll> data;
    private final OnClick cb;

    public PollsRecycle(List<Poll> data, OnClick cb) {
        this.data = data;
        this.cb = cb;
        setHasStableIds(false);
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    @Override
    public int getItemViewType(int position) {
        String code = toCode(data.get(position).type);
        if ("YES_NO".equals(code)) return VT_YESNO;
        if ("RATING".equals(code)) return VT_RATING;
        if ("MULTIPLE_CHOICE".equals(code)) return VT_MULTIPLE;
        return VT_RANK;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        @LayoutRes int layout;
        if (viewType == VT_YESNO) {
            layout = R.layout.activity_poll1;
        } else if (viewType == VT_RATING) {
            layout = R.layout.activity_poll2;
        } else if (viewType == VT_MULTIPLE) {
            layout = R.layout.activity_poll3;
        } else {
            layout = R.layout.activity_poll4;
        }
        View v = LayoutInflater.from(parent.getContext()).inflate(layout, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Poll p = data.get(position);

        String title = "";
        if (p.title != null) {
            title = p.title;
        }

        String code = toCode(p.type);
        String pretty = toPretty(code);

        if (h.title != null) {
            String display = title;
            if (p.isPrivate()) {
                display = "🔒 " + display; // show lock for private polls
            }
            h.title.setText(display);
        }

        if (h.subtitle != null) {
            h.subtitle.setText(pretty);
        }
        if (h.status != null) {
            if (p.now) {
                h.status.setText("Online");
            } else {
                h.status.setText("Closed");
            }
        }

        h.itemView.setOnClickListener(v -> cb.onClick(p));
    }

    private String toCode(String raw) {
        if (raw == null) return "YES_NO";
        String s = raw.trim();
        if (s.isEmpty()) return "YES_NO";
        s = s.replace(' ', '_');
        s = s.replace('-', '_');
        s = s.toUpperCase();
        if (s.equals("YES_OR_NO")) s = "YES_NO";
        return s;
    }

    private String toPretty(String code) {
        if (code == null) return "";
        if (code.equals("YES_NO")) return "YES OR NO";
        if (code.equals("MULTIPLE_CHOICE")) return "MULTIPLE_CHOICE";
        if (code.equals("RANKING")) return "RANKING";
        if (code.equals("RATING")) return "RATING";
        return code.replace('_', ' ');
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView title, subtitle;
        Chip status;

        VH(@NonNull View v) {
            super(v);
            title = v.findViewById(R.id.rowTitle);
            subtitle = v.findViewById(R.id.rowSubtitle);
            status = v.findViewById(R.id.chipStatus);
        }
    }
}

