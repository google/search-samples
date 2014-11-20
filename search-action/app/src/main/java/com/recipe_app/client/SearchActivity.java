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
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.actions.SearchIntents;
import com.recipe_app.R;
import com.recipe_app.client.content_provider.RecipeContentProvider;
import com.recipe_app.client.database.RecipeDatabaseHelper;
import com.recipe_app.client.database.RecipeTable;
import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import it.gmariotti.cardslib.library.internal.Card;
import it.gmariotti.cardslib.library.internal.CardGridArrayAdapter;
import it.gmariotti.cardslib.library.internal.CardHeader;
import it.gmariotti.cardslib.library.internal.CardThumbnail;
import it.gmariotti.cardslib.library.internal.base.BaseCard;
import it.gmariotti.cardslib.library.view.CardGridView;

/**
 * This Activity class is used to display a grid of search results for recipe searches.
 */
public class SearchActivity extends Activity implements SearchView.OnQueryTextListener {

    private SearchView mSearchView;
    private String mQuery;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().requestFeature(Window.FEATURE_ACTION_BAR);
        setContentView(R.layout.activity_search);

        // Create the database if it doesn't exist yet
        RecipeDatabaseHelper recipeDbHelper = new RecipeDatabaseHelper(this);
        try {
            recipeDbHelper.createDataBase();
        } catch (IOException ioe) {
            throw new Error("Unable to create database");
        }

        onNewIntent(getIntent());
    }

    protected void onNewIntent(Intent intent) {
        String action = intent.getAction();
        if (action.equals(Intent.ACTION_SEARCH) ||
            action.equals(SearchIntents.ACTION_SEARCH)) {
            mQuery = intent.getStringExtra(SearchManager.QUERY);
            doSearch(mQuery);
        }
    }

    private void doSearch(String query) {
        List<Card> cards = new ArrayList<Card>();
        Uri searchUri = RecipeContentProvider.CONTENT_URI.buildUpon()
                .appendPath("search").appendEncodedPath(query).build();

        Log.d("Search URI", searchUri.toString());

        String[] projection = { RecipeTable.ID, RecipeTable.TITLE,
                RecipeTable.DESCRIPTION, RecipeTable.PHOTO,
                RecipeTable.PREP_TIME};
        Cursor cursor = getContentResolver().query(searchUri, projection, null, null, null);
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            final Recipe recipe = Recipe.fromCursor(cursor);
            GplayGridCard card = new GplayGridCard(this, recipe);
            card.init();
            cards.add(card);
            cursor.moveToNext();
        }

        CardGridArrayAdapter mCardArrayAdapter = new CardGridArrayAdapter(this, cards);
        CardGridView listView = (CardGridView) findViewById(R.id.carddemo_grid_base1);
        if (listView != null) {
            listView.setAdapter(mCardArrayAdapter);
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
    }

    public boolean onQueryTextChange(String newText) {
        return false;
    }

    public boolean onQueryTextSubmit(String query) {
        return false;
    }

    public boolean onClose() {
        return false;
    }


    public class GplayGridCard extends Card {

        protected Recipe recipe;

        public GplayGridCard(Context context, Recipe recipe) {
            super(context, R.layout.carddemo_gplay_inner_content);
            this.recipe = recipe;
        }

        public GplayGridCard(Context context, int innerLayout) {
            super(context, innerLayout);
        }

        private void init() {
            CardHeader header = new CardHeader(getContext());
            header.setButtonOverflowVisible(true);
            header.setTitle(recipe.getTitle());

            addCardHeader(header);

            GplayGridThumb thumbnail = new GplayGridThumb(getContext());
            addCardThumbnail(thumbnail);

            setOnClickListener(new OnCardClickListener() {
                @Override
                public void onClick(Card card, View view) {
                    Intent intent = recipe.getViewIntent(view.getContext());
                    startActivity(intent);
                }
            });
        }

        class GplayGridThumb extends CardThumbnail {

            public GplayGridThumb(Context context) {
                super(context);
            }

            @Override
            public void setupInnerViewElements(ViewGroup parent, View viewImage) {

                Log.d("Thumnail image", recipe.getPhoto());
                Picasso.with(getContext())
                        .load(recipe.getPhoto())
                        .into((ImageView)viewImage);

                viewImage.getLayoutParams().width = 250;
                viewImage.getLayoutParams().height = 250;
            }
        }

    }
}
