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
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.appindexing.Action;
import com.google.firebase.appindexing.FirebaseAppIndex;
import com.google.firebase.appindexing.FirebaseUserActions;
import com.google.firebase.appindexing.Indexable;
import com.google.firebase.appindexing.builders.Actions;
import com.google.firebase.appindexing.builders.Indexables;
import com.recipe_app.R;
import com.recipe_app.client.Recipe.Note;
import com.recipe_app.client.content_provider.RecipeContentProvider;
import com.recipe_app.client.database.RecipeNoteTable;
import com.recipe_app.client.database.RecipeTable;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

/**
 * This Activity class is used to display a {@link com.recipe_app.client.Recipe} object
 */
public class RecipeActivity extends Activity {

    private static final String TAG = RecipeActivity.class.getName();

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

    private Recipe mRecipe;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recipe);

        onNewIntent(getIntent());
    }

    // [START on_start]
    @Override
    public void onStart() {
        super.onStart();
        if (mRecipe != null) {
            // If you’re logging an action on content that hasn’t been added to the index yet,
            // add it first with FirebaseAppIndex.getInstance().update(getIndexable());
            // indexRecipe()
            FirebaseUserActions.getInstance().start(getRecipeViewAction());
        }
    }

    @Override
    public void onStop() {
        if (mRecipe != null) {
            FirebaseUserActions.getInstance().end(getRecipeViewAction());
        }
        super.onStop();
    }
    // [END on_start]

    protected void onNewIntent(Intent intent) {
        String action = intent.getAction();
        String data = intent.getDataString();
        if (Intent.ACTION_VIEW.equals(action) && data != null) {
            if (data.endsWith("note")) {
                data = data.substring(0, data.lastIndexOf("/"));
            }
            String recipeId = data.substring(data.lastIndexOf("/") + 1);
            Uri contentUri = RecipeContentProvider.CONTENT_URI.buildUpon()
                    .appendPath(recipeId).build();
            showRecipe(contentUri);
        }
    }

    // [START index_note]
    private void indexNote() {
        Note note = mRecipe.getNote();
        Indexable noteToIndex = Indexables.noteDigitalDocumentBuilder()
                .setName(mRecipe.getTitle() + " Note")
                .setText(note.getText())
                .setUrl(mRecipe.getNoteUrl())
                .build();

        Task<Void> task = FirebaseAppIndex.getInstance().update(noteToIndex);
        task.addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                Log.d(TAG, "App Indexing API: Successfully added note to index");
            }
        });

        task.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                Log.e(TAG, "App Indexing API: Failed to add note to index. " + exception
                        .getMessage());
            }
        });
    }
    // [END index_note]

    private void indexRecipe() {
        Indexable recipeToIndex = new Indexable.Builder()
                .setName(mRecipe.getTitle())
                .setUrl(mRecipe.getRecipeUrl())
                .setImage(mRecipe.getPhoto())
                .setDescription(mRecipe.getDescription())
                .build();

        Task<Void> task = FirebaseAppIndex.getInstance().update(recipeToIndex);
        task.addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                Log.d(TAG, "App Indexing API: Successfully added " + mRecipe.getTitle() + " to " +
                        "index");
            }
        });

        task.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                Log.e(TAG, "App Indexing API: Failed to add " + mRecipe.getTitle() + " to index. " +
                        "" + exception.getMessage());
            }
        });
    }

    private void showRecipe(Uri recipeUri) {
        Log.d("Recipe Uri", recipeUri.toString());

        String[] projection = {RecipeTable.ID, RecipeTable.TITLE,
                RecipeTable.DESCRIPTION, RecipeTable.PHOTO,
                RecipeTable.PREP_TIME};
        Cursor cursor = getContentResolver().query(recipeUri, projection, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {

            mRecipe = Recipe.fromCursor(cursor);

            Uri ingredientsUri = RecipeContentProvider.CONTENT_URI.buildUpon().appendPath
                    ("ingredients").appendPath(mRecipe.getId()).build();
            Cursor ingredientsCursor = getContentResolver().query(ingredientsUri, projection,
                    null, null, null);
            if (ingredientsCursor != null && ingredientsCursor.moveToFirst()) {
                do {
                    Recipe.Ingredient ingredient = new Recipe.Ingredient();
                    ingredient.setAmount(ingredientsCursor.getString(0));
                    ingredient.setDescription(ingredientsCursor.getString(1));
                    mRecipe.addIngredient(ingredient);
                    ingredientsCursor.moveToNext();
                } while (!ingredientsCursor.isAfterLast());
                ingredientsCursor.close();
            }

            Uri instructionsUri = RecipeContentProvider.CONTENT_URI.buildUpon().appendPath
                    ("instructions").appendPath(mRecipe.getId()).build();
            Cursor instructionsCursor = getContentResolver().query(instructionsUri, projection,
                    null, null, null);
            if (instructionsCursor != null && instructionsCursor.moveToFirst()) {
                do {
                    Recipe.Step step = new Recipe.Step();
                    step.setDescription(instructionsCursor.getString(1));
                    step.setPhoto(instructionsCursor.getString(2));
                    mRecipe.addStep(step);
                    instructionsCursor.moveToNext();
                } while (!instructionsCursor.isAfterLast());
                instructionsCursor.close();
            }

            Uri noteUri = RecipeContentProvider.CONTENT_URI.buildUpon().appendPath("notes")
                    .appendPath(mRecipe.getId()).build();
            Cursor noteCursor = getContentResolver().query(noteUri, projection, null, null, null);
            if (noteCursor != null && noteCursor.moveToFirst()) {
                Note note = Note.fromCursor(noteCursor);
                mRecipe.setNote(note);
                noteCursor.close();
            }

            // always close the cursor
            cursor.close();
        } else {
            Toast toast = Toast.makeText(getApplicationContext(),
                    "No match for deep link " + recipeUri.toString(),
                    Toast.LENGTH_SHORT);
            toast.show();
        }

        if (mRecipe != null) {
            // Create the adapter that will return a fragment for each of the steps of the recipe.
            mSectionsPagerAdapter = new SectionsPagerAdapter(getFragmentManager());

            // Set up the ViewPager with the sections adapter.
            mViewPager = (ViewPager) findViewById(R.id.pager);
            mViewPager.setAdapter(mSectionsPagerAdapter);

            // Set the recipe title
            TextView recipeTitle = (TextView) findViewById(R.id.recipeTitle);
            recipeTitle.setText(mRecipe.getTitle());

            // Set the recipe prep time
            TextView recipeTime = (TextView) findViewById(R.id.recipeTime);
            recipeTime.setText("  " + mRecipe.getPrepTime());

            //Set the note button toggle
            ToggleButton addNoteToggle = (ToggleButton) findViewById(R.id.addNoteToggle);
            addNoteToggle.setChecked(mRecipe.getNote() != null);
            addNoteToggle.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (mRecipe.getNote() != null) {
                        displayNoteDialog(getString(R.string.dialog_update_note), getString(R
                                .string.dialog_delete_note));
                    } else {
                        displayNoteDialog(getString(R.string.dialog_add_note), getString(R.string
                                .dialog_cancel_note));
                    }
                }
            });
        }
    }

    // [START end_note_view_action]
    private void displayNoteDialog(final String positiveText, final String negativeText) {
        final AlertDialog.Builder addNoteDialog = new AlertDialog.Builder(this);
        final EditText edittext = new EditText(this);
        // [START_EXCLUDE]
        addNoteDialog.setTitle("Enter a note");
        addNoteDialog.setView(edittext);
        if (mRecipe.getNote() != null) {
            edittext.setText(mRecipe.getNote().getText());
        }

        addNoteDialog.setPositiveButton(positiveText, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                // [END_EXCLUDE]
                FirebaseUserActions.getInstance().end(getNoteCommentAction());
                // [START_EXCLUDE]

                String noteText = edittext.getText().toString().trim();
                ContentValues values = new ContentValues();
                values.put(RecipeNoteTable.TEXT_COLUMN, noteText);

                Uri noteUri = RecipeContentProvider.CONTENT_URI.buildUpon().appendPath("notes")
                        .appendPath(mRecipe.getId()).build();
                if (getString(R.string.dialog_add_note).equalsIgnoreCase(positiveText)) {
                    getContentResolver().insert(noteUri, values);
                } else {
                    getContentResolver().update(noteUri, values, null, null);
                }

                Note note = new Note();
                note.setText(noteText);
                mRecipe.setNote(note);
                ((ToggleButton) findViewById(R.id.addNoteToggle)).setChecked(true);
                indexNote();
            }
        });

        addNoteDialog.setNegativeButton(negativeText, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                FirebaseUserActions.getInstance().end(getNoteCommentAction());

                if (getString(R.string.dialog_delete_note).equalsIgnoreCase(negativeText)) {
                    Uri noteUri = RecipeContentProvider.CONTENT_URI.buildUpon().appendPath("notes")
                            .appendPath(mRecipe.getId()).build();
                    getContentResolver().delete(noteUri, null, null);
                    mRecipe.setNote(null);
                    ((ToggleButton) findViewById(R.id.addNoteToggle)).setChecked(false);

                    // [START remove_note]
                    // Deletes or removes the corresponding notes from index.
                    String noteUrl = mRecipe.getNoteUrl();
                    FirebaseAppIndex.getInstance().remove(noteUrl);
                    // [END remove_note]
                } else {
                    dialog.dismiss();
                    ((ToggleButton) findViewById(R.id.addNoteToggle)).setChecked(mRecipe.getNote
                            () != null);
                }
            }
        });
        addNoteDialog.show();
        FirebaseUserActions.getInstance().start(getNoteCommentAction());
        // [END_EXCLUDE]
    }

    private Action getNoteCommentAction() {
        return new Action.Builder(Action.Builder.COMMENT_ACTION)
                .setObject(mRecipe.getTitle() + " Note", mRecipe.getNoteUrl())
                .setMetadata(new Action.Metadata.Builder().setUpload(false))
                .build();
    }
    // [END end_note_view_action]

    private Action getRecipeViewAction() {
        return Actions.newView(mRecipe.getTitle(), mRecipe.getRecipeUrl());
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

        public RecipeFragment() {
        }

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

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_recipe, container, false);

            this.recipe = ((RecipeActivity) getActivity()).mRecipe;

            progressBar = (ProgressBar) rootView.findViewById(R.id.loading);
            recipeImage = (ImageView) rootView.findViewById(R.id.recipe_image);

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
                Fragment ingredientsFragment = IngredientsFragment.newInstance(recipe,
                        sectionNumber);
                transaction.replace(R.id.ingredients_fragment, ingredientsFragment).commit();
            } else {
                Fragment instructionFragment = InstructionFragment.newInstance(recipe,
                        sectionNumber);
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

        public IngredientsFragment() {
        }

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

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.ingredients_fragment, container, false);

            this.recipe = ((RecipeActivity) getActivity()).mRecipe;

            TableLayout table = (TableLayout) rootView.findViewById(R.id.ingredientsTable);
            for (Recipe.Ingredient ingredient : recipe.getIngredients()) {
                TableRow row = (TableRow) inflater.inflate(R.layout.ingredients_row, null);
                ((TextView) row.findViewById(R.id.attrib_name)).setText(ingredient.getAmount());
                ((TextView) row.findViewById(R.id.attrib_value)).setText(ingredient
                        .getDescription());
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

        public InstructionFragment() {
        }

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

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.instructions_fragment, container, false);
            int sectionNumber = this.getArguments().getInt(ARG_SECTION_NUMBER);

            this.recipe = ((RecipeActivity) getActivity()).mRecipe;

            TextView instructionTitle = (TextView) rootView.findViewById(R.id.instructionTitle);
            instructionTitle.setText("Step " + Integer.toString(sectionNumber - 1));

            // Section 1 is ingredients list, Section 2 is start of instructions
            // So the first instruction [instructions.get(0)] will be on Section 2. Second
            // instruction [instructions.get(1)] on will be on Section 3...
            Recipe.Step step = recipe.getInstructions().get(sectionNumber - 2);
            TextView instructionBody = (TextView) rootView.findViewById(R.id.instructionBody);
            instructionBody.setText(step.getDescription());

            return rootView;
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
            return RecipeFragment.newInstance(mRecipe, position + 1);
        }

        @Override
        public int getCount() {
            if (mRecipe != null) {
                return mRecipe.getInstructions().size() + 1;
            } else {
                return 0;
            }
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return null;
        }
    }

}