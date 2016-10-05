package com.recipe_app.client;

import android.app.IntentService;
import android.content.Intent;

import com.google.firebase.appindexing.FirebaseAppIndex;
import com.google.firebase.appindexing.Indexable;
import com.google.firebase.appindexing.builders.Indexables;

import com.recipe_app.client.Recipe.Note;

import java.util.ArrayList;
import java.util.List;


public class AppIndexingService extends IntentService {

    public AppIndexingService() {
        super("AppIndexingService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {

    }

    private List<Recipe> getAllRecipes() {
        ArrayList recipesList = new ArrayList();
        // TODO: Exercise - access all recipes with their notes from the database here.
        return recipesList;
    }
}
