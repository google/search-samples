package com.recipe_app.client;

import android.app.IntentService;
import android.content.Intent;

import com.google.firebase.appindexing.FirebaseAppIndex;
import com.google.firebase.appindexing.Indexable;
import com.google.firebase.appindexing.builders.Indexables;

import com.recipe_app.client.Recipe.Note;

import java.util.ArrayList;
import java.util.List;

// [START app_index_service]
public class AppIndexingService extends IntentService {

    public AppIndexingService() {
        super("AppIndexingService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        ArrayList<Indexable> indexableNotes = new ArrayList<>();

        for (Recipe recipe : getAllRecipes()) {
            Note note = recipe.getNote();
            if (note != null) {
                Indexable noteToIndex = Indexables.noteDigitalDocumentBuilder()
                        .setName(recipe.getTitle() + " Note")
                        .setText(note.getText())
                        .setUrl(recipe.getNoteUrl())
                        .build();

                indexableNotes.add(noteToIndex);
            }
        }

        if (indexableNotes.size() > 0) {
            Indexable[] notesArr = new Indexable[indexableNotes.size()];
            notesArr = indexableNotes.toArray(notesArr);

            // batch insert indexable notes into index
            FirebaseAppIndex.getInstance().update(notesArr);
        }
    }

    // [START_EXCLUDE]
    private List<Recipe> getAllRecipes() {
        ArrayList recipesList = new ArrayList();
        // Code access all recipes with their notes from the database here.
        return recipesList;
    }
    // [END_EXCLUDE]
}
// [END app_index_service]