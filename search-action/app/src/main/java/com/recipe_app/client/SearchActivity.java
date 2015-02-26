/*
 * Copyright 2014 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.recipe_app.client;

import android.app.Activity;
import android.app.SearchManager;
import android.app.SearchableInfo;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.Window;
import android.widget.SearchView;

import com.recipe_app.R;
import com.recipe_app.client.content_provider.RecipeContentProvider;
import com.recipe_app.client.database.RecipeTable;

import java.util.List;

/**
 * This Activity class is used to display a grid of search results for recipe searches.
 */
public class SearchActivity extends Activity implements SearchView.OnQueryTextListener {

    private static String GMS_SEARCH_ACTION = "com.google.android.gms.actions.SEARCH_ACTION";

    private RecyclerView mRecyclerView;
    private SearchResultAdapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;

    private SearchView mSearchView;
    private String mQuery;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().requestFeature(Window.FEATURE_ACTION_BAR);
        setContentView(R.layout.activity_search);

        mRecyclerView = (RecyclerView) findViewById(R.id.my_recycler_view);

        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        mRecyclerView.setHasFixedSize(true);

        // use a grid layout manager
        int numColumns = getResources().getInteger(R.integer.search_results_columns);
        mLayoutManager = new GridLayoutManager(this, numColumns);
        mRecyclerView.setLayoutManager(mLayoutManager);

        // specify an adapter (see also next example)
        mAdapter = new SearchResultAdapter();
        mRecyclerView.setAdapter(mAdapter);

        onNewIntent(getIntent());
    }

    protected void onNewIntent(Intent intent) {
        String action = intent.getAction();
        if (action.equals(Intent.ACTION_SEARCH) ||
            action.equals(GMS_SEARCH_ACTION)) {
            mQuery = intent.getStringExtra(SearchManager.QUERY);
            doSearch(mQuery);
        }
    }

    private void doSearch(String query) {
        Uri searchUri = RecipeContentProvider.CONTENT_URI.buildUpon()
                .appendPath("search").appendEncodedPath(query).build();

        String[] projection = { RecipeTable.ID, RecipeTable.TITLE,
                RecipeTable.DESCRIPTION, RecipeTable.PHOTO,
                RecipeTable.PREP_TIME};
        Cursor cursor = getContentResolver().query(searchUri, projection, null, null, null);
        cursor.moveToFirst();
        mAdapter.clearResults();
        while (!cursor.isAfterLast()) {
            final Recipe recipe = Recipe.fromCursor(cursor);
            mAdapter.addResult(recipe);
            cursor.moveToNext();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.searchview_in_menu, menu);
        MenuItem searchItem = menu.findItem(R.id.action_search);
        mSearchView = (SearchView) searchItem.getActionView();
        setupSearchView(searchItem);

        if (mQuery != null) {
            mSearchView.setQuery(mQuery, false);
        }

        return true;
    }

    private void setupSearchView(MenuItem searchItem) {

        mSearchView.setIconifiedByDefault(false);

        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        if (searchManager != null) {
            List<SearchableInfo> searchables = searchManager.getSearchablesInGlobalSearch();

            SearchableInfo info = searchManager.getSearchableInfo(getComponentName());
            for (SearchableInfo inf : searchables) {
                if (inf.getSuggestAuthority() != null
                        && inf.getSuggestAuthority().startsWith("applications")) {
                    info = inf;
                }
            }
            mSearchView.setSearchableInfo(info);
        }

        mSearchView.setOnQueryTextListener(this);
        mSearchView.setFocusable(false);
        mSearchView.setFocusableInTouchMode(false);
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        return false;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        return false;
    }
}
