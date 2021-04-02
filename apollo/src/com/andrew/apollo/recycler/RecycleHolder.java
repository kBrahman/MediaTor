package com.andrew.apollo.recycler;

import android.view.View;
import android.widget.AbsListView.RecyclerListener;

import com.andrew.apollo.ui.MusicViewHolder;

public class RecycleHolder implements RecyclerListener {

    /**
     * {@inheritDoc}
     */
    @Override
    public void onMovedToScrapHeap(final View view) {
        if (view == null) {
            return;
        }

        MusicViewHolder holder = (MusicViewHolder) view.getTag();
        if (holder == null) {
            holder = new MusicViewHolder(view);
            view.setTag(holder);
        }

        holder.reset();
    }
}
