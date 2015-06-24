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
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.recipe_app.R;
import com.recipe_app.client.content_provider.RecipeContentProvider;
import com.recipe_app.client.database.RecipeTable;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

/**
 * This Activity class is used to display a {@link com.recipe_app.client.Recipe} object
 */
public class RecipeActivity extends Activity {

    private static final String TAG = RecipeActivity.class.getName();
    private static final Uri BASE_APP_URI = Uri.parse("android-app://com.recipe_app/http/recipe-app.com/recipe/");

    private GoogleApiClient mClient;

    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link android.support.v13.app.FragmentStatePagerAdapter}.
     */
    private SectionsPagerAdapter mSectionsPagerAdapter;

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    private ViewPager mViewPager;

    private Recipe recipe;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recipe);

        mClient = new GoogleApiClient.Builder(this).addApi(AppIndex.APP_INDEX_API).build();

        onNewIntent(getIntent());
    }

    protected void onNewIntent(Intent intent) {
        String action = intent.getAction();
        String data = intent.getDataString();
        if (Intent.ACTION_VIEW.equals(action) && data != null) {
            String recipeId = data.substring(data.lastIndexOf("/") + 1);
            Uri contentUri = RecipeContentProvider.CONTENT_URI.buildUpon()
                    .appendPath(recipeId).build();
            showRecipe(contentUri);
        }
    }

    @Override
    public void onStart(){
        super.onStart();
        if (recipe != null) {
            // Connect your client
            mClient.connect();

            // Define a title for your current page, shown in autocompletion UI
            final String TITLE = recipe.getTitle();
            final Uri APP_URI = BASE_APP_URI.buildUpon().appendPath(recipe.getId()).build();

            Action viewAction = Action.newAction(Action.TYPE_VIEW, TITLE, APP_URI);

            // Call the App Indexing API view method
            PendingResult<Status> result = AppIndex.AppIndexApi.start(mClient, viewAction);

            result.setResultCallback(new ResultCallback<Status>() {
                @Override
                public void onResult(Status status) {
                    if (status.isSuccess()) {
                        Log.d(TAG, "App Indexing API: Recorded recipe "
                                + recipe.getTitle() + " view successfully.");
                    } else {
                        Log.e(TAG, "App Indexing API: There was an error recording the recipe view."
                                + status.toString());
                    }
                }
            });
        }
    }

    @Override
    public void onStop(){
        if (recipe != null) {
            final String TITLE = recipe.getTitle();
            final Uri APP_URI = BASE_APP_URI.buildUpon().appendPath(recipe.getId()).build();

            Action viewAction = Action.newAction(Action.TYPE_VIEW, TITLE, APP_URI);
            PendingResult<Status> result = AppIndex.AppIndexApi.end(mClient, viewAction);

            result.setResultCallback(new ResultCallback<Status>() {
                @Override
                public void onResult(Status status) {
                    if (status.isSuccess()) {
                        Log.d(TAG, "App Indexing API: Recorded recipe "
                                + recipe.getTitle() + " view end successfully.");
                    } else {
                        Log.e(TAG, "App Indexing API: There was an error recording the recipe view."
                                + status.toString());
                    }
                }
            });

            mClient.disconnect();
            super.onStop();
        }
    }

    private void showRecipe(Uri recipeUri) {
        Log.d("Recipe Uri", recipeUri.toString());

        String[] projection = { RecipeTable.ID, RecipeTable.TITLE,
                RecipeTable.DESCRIPTION, RecipeTable.PHOTO,
                RecipeTable.PREP_TIME};
        Cursor cursor = getContentResolver().query(recipeUri, projection, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {

            recipe = Recipe.fromCursor(cursor);

            Uri ingredientsUri = RecipeContentProvider.CONTENT_URI.buildUpon().appendPath("ingredients").appendPath(recipe.getId()).build();
            Cursor ingredientsCursor = getContentResolver().query(ingredientsUri, projection, null, null, null);
            if (ingredientsCursor != null && ingredientsCursor.moveToFirst()) {
                do {
                    Recipe.Ingredient ingredient = new Recipe.Ingredient();
                    ingredient.setAmount(ingredientsCursor.getString(0));
                    ingredient.setDescription(ingredientsCursor.getString(1));
                    recipe.addIngredient(ingredient);
                    ingredientsCursor.moveToNext();
                } while (!ingredientsCursor.isAfterLast());
                ingredientsCursor.close();
            }

            Uri instructionsUri = RecipeContentProvider.CONTENT_URI.buildUpon().appendPath("instructions").appendPath(recipe.getId()).build();
            Cursor instructionsCursor = getContentResolver().query(instructionsUri, projection, null, null, null);
            if (instructionsCursor != null && instructionsCursor.moveToFirst()) {
                do {
                    Recipe.Step step = new Recipe.Step();
                    step.setDescription(instructionsCursor.getString(1));
                    step.setPhoto(instructionsCursor.getString(2));
                    recipe.addStep(step);
                    instructionsCursor.moveToNext();
                } while (!instructionsCursor.isAfterLast());
                instructionsCursor.close();
            }

            // always close the cursor
            cursor.close();
        } else {
            Toast toast = Toast.makeText(getApplicationContext(),
                    "No match for deep link " + recipeUri.toString(),
                    Toast.LENGTH_SHORT);
            toast.show();
        }

        if (recipe != null) {
            // Create the adapter that will return a fragment for each of the steps of the recipe.
            mSectionsPagerAdapter = new SectionsPagerAdapter(getFragmentManager());

            // Set up the ViewPager with the sections adapter.
            mViewPager = (ViewPager) findViewById(R.id.pager);
            mViewPager.setAdapter(mSectionsPagerAdapter);

            // Set the recipe title
            TextView recipeTitle = (TextView) findViewById(R.id.recipeTitle);
            recipeTitle.setText(recipe.getTitle());

            // Set the recipe prep time
            TextView recipeTime = (TextView) findViewById(R.id.recipeTime);
            recipeTime.setText("  " + recipe.getPrepTime());
        }
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            // Return a PlaceholderFragment (defined as a static inner class below).
            return RecipeFragment.newInstance(recipe, position + 1);
        }

        @Override
        public int getCount() {
            if (recipe != null) {
                return recipe.getInstructions().size() + 1;
            } else {
                return 0;
            }
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return null;
        }
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class RecipeFragment extends Fragment {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private static final String ARG_SECTION_NUMBER = "section_number";

        private Recipe recipe;
        private ProgressBar progressBar;
        private ImageView recipeImage;

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        public static RecipeFragment newInstance(Recipe recipe, int sectionNumber) {
            RecipeFragment fragment = new RecipeFragment();
            fragment.recipe = recipe;
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }

        public RecipeFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_recipe, container, false);

            this.recipe = ((RecipeActivity)getActivity()).recipe;


            progressBar = (ProgressBar) rootView.findViewById(R.id.loading);
            recipeImage = (ImageView)rootView.findViewById(R.id.recipe_image);

            String photoUrl = recipe.getPhoto();

            int sectionNumber = this.getArguments().getInt(ARG_SECTION_NUMBER);
            if (sectionNumber > 1) {
                Recipe.Step step = recipe.getInstructions().get(sectionNumber - 2);
                if (step.getPhoto() != null) {
                    photoUrl = step.getPhoto();
                }
            }

            Picasso.with(rootView.getContext())
                .load(photoUrl)
                .into(recipeImage, new Callback.EmptyCallback() {
                    @Override
                    public void onSuccess() {
                        progressBar.setVisibility(View.GONE);
                        Log.d("Picasso", "Image loaded successfully");
                    }

                    @Override
                    public void onError() {
                        progressBar.setVisibility(View.GONE);
                        Log.d("Picasso", "Failed to load image");
                    }
                });

            FragmentTransaction transaction = getChildFragmentManager().beginTransaction();

            if (sectionNumber == 1) {
                Fragment ingredientsFragment = IngredientsFragment.newInstance(recipe, sectionNumber);
                transaction.replace(R.id.ingredients_fragment, ingredientsFragment).commit();
            } else {
                Fragment instructionFragment = InstructionFragment.newInstance(recipe, sectionNumber);
                transaction.replace(R.id.instruction_fragment, instructionFragment).commit();
            }

            return rootView;
        }
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class IngredientsFragment extends Fragment {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private static final String ARG_SECTION_NUMBER = "section_number";
        private Recipe recipe;

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        public static IngredientsFragment newInstance(Recipe recipe, int sectionNumber) {
            IngredientsFragment fragment = new IngredientsFragment();
            fragment.recipe = recipe;
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }

        public IngredientsFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.ingredients_fragment, container, false);

            this.recipe = ((RecipeActivity)getActivity()).recipe;

            TableLayout table = (TableLayout)rootView.findViewById(R.id.ingredientsTable);
            for (Recipe.Ingredient ingredient : recipe.getIngredients()) {
                TableRow row = (TableRow)inflater.inflate(R.layout.ingredients_row, null);
                ((TextView)row.findViewById(R.id.attrib_name)).setText(ingredient.getAmount());
                ((TextView)row.findViewById(R.id.attrib_value)).setText(ingredient.getDescription());
                table.addView(row);
            }

            return rootView;
        }
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class InstructionFragment extends Fragment {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private static final String ARG_SECTION_NUMBER = "section_number";
        private Recipe recipe;

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        public static InstructionFragment newInstance(Recipe recipe, int sectionNumber) {
            InstructionFragment fragment = new InstructionFragment();
            fragment.recipe = recipe;
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }

        public InstructionFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.instructions_fragment, container, false);
            int sectionNumber = this.getArguments().getInt(ARG_SECTION_NUMBER);

            this.recipe = ((RecipeActivity)getActivity()).recipe;

            TextView instructionTitle = (TextView)rootView.findViewById(R.id.instructionTitle);
            instructionTitle.setText("Step " + Integer.toString(sectionNumber - 1));

            Recipe.Step step = recipe.getInstructions().get(sectionNumber - 2);
            TextView instructionBody = (TextView)rootView.findViewById(R.id.instructionBody);
            instructionBody.setText(step.getDescription());

            return rootView;
        }
    }

}
