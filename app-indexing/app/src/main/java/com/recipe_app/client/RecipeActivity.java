package com.recipe_app.client;

import java.io.IOException;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.support.v13.app.FragmentPagerAdapter;
import android.os.Bundle;
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

import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.recipe_app.R;
import com.recipe_app.client.content_provider.RecipeContentProvider;
import com.recipe_app.client.database.RecipeDatabaseHelper;
import com.recipe_app.client.database.RecipeTable;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;


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
    SectionsPagerAdapter mSectionsPagerAdapter;

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    ViewPager mViewPager;

    private Recipe recipe;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recipe);

        mClient = new GoogleApiClient.Builder(this).addApi(AppIndex.APP_INDEX_API).build();

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
        // Connect your client
        mClient.connect();

        // Define a title for your current page, shown in autocompletion UI
        final String TITLE = recipe.getTitle();
        final Uri APP_URI = BASE_APP_URI.buildUpon().appendPath(recipe.getId()).build();
        final Uri WEB_URL = Uri.parse(recipe.getUrl());

        // Call the App Indexing API view method
        PendingResult<Status> result = AppIndex.AppIndexApi.view(mClient, this,
                APP_URI, TITLE, WEB_URL, null);

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

    @Override
    public void onStop(){
        super.onStop();

        final Uri APP_URI = BASE_APP_URI.buildUpon().appendPath(recipe.getId()).build();
        PendingResult<Status> result = AppIndex.AppIndexApi.viewEnd(mClient, this, APP_URI);

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
    }

    private void showRecipe(Uri recipeUri) {
        Log.d("Recipe Uri", recipeUri.toString());

        String[] projection = { RecipeTable.ID, RecipeTable.TITLE,
                RecipeTable.DESCRIPTION, RecipeTable.PHOTO,
                RecipeTable.PREP_TIME};
        Cursor cursor = getContentResolver().query(recipeUri, projection, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {

            recipe = new Recipe(cursor.getString(0));
            recipe.setTitle(cursor.getString(1));
            recipe.setDescription(cursor.getString(2));
            recipe.setPhoto(cursor.getString(3));
            recipe.setPrepTime(cursor.getString(4));

            String recipeId = recipeUri.getLastPathSegment();

            Uri ingredientsUri = RecipeContentProvider.CONTENT_URI.buildUpon().appendPath("ingredients").appendPath(recipeId).build();
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

            Uri instructionsUri = RecipeContentProvider.CONTENT_URI.buildUpon().appendPath("instructions").appendPath(recipeId).build();
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
        }

        // Create the adapter that will return a fragment for each of the steps of the recipe.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        // Set the recipe title
        TextView recipeTitle = (TextView)findViewById(R.id.recipeTitle);
        recipeTitle.setText(recipe.getTitle());

        // Set the recipe prep time
        TextView recipeTime = (TextView)findViewById(R.id.recipeTime);
        recipeTime.setText("  " + recipe.getPrepTime());
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.recipe, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
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
            return recipe.getInstructions().size() + 1;
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

            TextView instructionTitle = (TextView)rootView.findViewById(R.id.instructionTitle);
            instructionTitle.setText("Step " + Integer.toString(sectionNumber - 1));

            Recipe.Step step = recipe.getInstructions().get(sectionNumber - 2);
            TextView instructionBody = (TextView)rootView.findViewById(R.id.instructionBody);
            instructionBody.setText(step.getDescription());

            return rootView;
        }
    }

}
