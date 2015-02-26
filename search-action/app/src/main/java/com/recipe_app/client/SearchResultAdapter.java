package com.recipe_app.client;

import android.content.Intent;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.recipe_app.R;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

public class SearchResultAdapter extends RecyclerView.Adapter<SearchResultAdapter.ViewHolder> {
    private List<Recipe> mDataset = new ArrayList<Recipe>();

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    public static class ViewHolder extends RecyclerView.ViewHolder {
        // each data item is just a string in this case
        public View mTextView;
        public ViewHolder(View v) {
            super(v);
            mTextView = v;
        }
    }

    // Provide a suitable constructor (depends on the kind of dataset)
    public SearchResultAdapter() {
    }

    // Create new views (invoked by the layout manager)
    @Override
    public SearchResultAdapter.ViewHolder onCreateViewHolder(ViewGroup parent,
                                                             int viewType) {
        // create a new view
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.search_result_card, parent, false);

        ViewHolder vh = new ViewHolder(v);
        return vh;
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        final Recipe recipe = mDataset.get(position);

        TextView cardTitle = (TextView)holder.mTextView.findViewById(R.id.info_text);
        cardTitle.setText(recipe.getTitle());

        ImageView cardThumb = (ImageView)holder.mTextView.findViewById(R.id.seach_result_thumbnail);
        Picasso.with(holder.itemView.getContext())
                .load(recipe.getPhoto())
                .fit().centerCrop()
                .into(cardThumb);

        holder.mTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = recipe.getViewIntent(view.getContext());
                view.getContext().startActivity(intent);
            }
        });
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return mDataset.size();
    }

    public void addResult(Recipe recipe) {
        mDataset.add(recipe);
    }

    public void clearResults() {
        mDataset.clear();
    }
}
