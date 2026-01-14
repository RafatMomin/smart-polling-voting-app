package com.example.frontendproject;

import android.view.LayoutInflater;
import android.widget.CheckBox;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.recyclerview.widget.RecyclerView;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class GroupBoxAdapterTest {

    @Test
    public void setData_replacesList_andItemCountMatches() {
        List<PollingWindow.GroupItem> initial = new ArrayList<>();
        initial.add(new PollingWindow.GroupItem(1, "Group A"));
        GroupBoxAdapter adapter = new GroupBoxAdapter(initial);

        assertEquals(1, adapter.getItemCount());

        List<PollingWindow.GroupItem> newList = Arrays.asList(
                new PollingWindow.GroupItem(2, "Group B"),
                new PollingWindow.GroupItem(3, "Group C")
        );
        adapter.setData(newList);

        assertEquals(2, adapter.getItemCount());
        assertEquals("Group B", newList.get(0).name);
    }

    @Test
    public void getSelectedIds_returnsCheckedIdsOnly() {
        List<PollingWindow.GroupItem> list = new ArrayList<>();
        PollingWindow.GroupItem g1 = new PollingWindow.GroupItem(10, "Alpha");
        PollingWindow.GroupItem g2 = new PollingWindow.GroupItem(20, "Beta");
        g1.checked = true;
        g2.checked = false;
        list.add(g1);
        list.add(g2);

        GroupBoxAdapter adapter = new GroupBoxAdapter(list);
        List<Integer> ids = adapter.getSelectedIds();

        assertEquals(1, ids.size());
        assertEquals(Integer.valueOf(10), ids.get(0));
    }

    @Test
    public void onCreateViewHolder_inflatesLayout_andBindsText() {
        List<PollingWindow.GroupItem> list = new ArrayList<>();
        list.add(new PollingWindow.GroupItem(5, "Gamma"));
        GroupBoxAdapter adapter = new GroupBoxAdapter(list);

        LayoutInflater inflater = LayoutInflater.from(ApplicationProvider.getApplicationContext());
        android.widget.FrameLayout parent = new android.widget.FrameLayout(ApplicationProvider.getApplicationContext());

        RecyclerView.ViewHolder vh = adapter.onCreateViewHolder(parent, 0);
        assertTrue(vh instanceof GroupBoxAdapter.VH);

        adapter.onBindViewHolder((GroupBoxAdapter.VH) vh, 0);
        CheckBox cb = ((GroupBoxAdapter.VH) vh).cb;
        assertEquals("Gamma", cb.getText().toString());

        cb.performClick(); // toggle checked state
        assertTrue(list.get(0).checked);
    }
}
