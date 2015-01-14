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

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;

import com.recipe_app.client.database.RecipeIngredientTable;
import com.recipe_app.client.database.RecipeInstructionsTable;
import com.recipe_app.client.database.RecipeTable;

import java.util.ArrayList;
import java.util.List;

/**
 * The {@link com.recipe_app.client.Recipe} class stores all the information about a recipe including a
 * list of ingredients and all the steps required to prepare it.
 */
public class Recipe {

    public static final String URL_BASE = "http://recipe-app.com/recipe/";

    private String id;
    private String title;
    private String photo;
    private String description;
    private String prepTime;

    private List<Ingredient> ingredients = new ArrayList<Ingredient>();
    private List<Step> instructions = new ArrayList<Step>();

    public Recipe(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getUrl() {
        return URL_BASE + this.id;
    }

    public String getPhoto() {
        return photo;
    }

    public void setPhoto(String photo) {
        this.photo = photo;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPrepTime() {
        return prepTime;
    }

    public void setPrepTime(String prepTime) {
        this.prepTime = prepTime;
    }

    public List<Ingredient> getIngredients() {
        return ingredients;
    }

    public void addIngredient(Ingredient ingredient) {
        ingredients.add(ingredient);
    }

    public List<Step> getInstructions() {
        return instructions;
    }

    public void addStep(Step step) {
        instructions.add(step);
    }

    public Intent getViewIntent(Context context) {
        Intent intent = new Intent(context, RecipeActivity.class);
        intent.setAction(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(getUrl()));
        return intent;
    }

    /**
     * Static helper method for populating attributes from a database cursor.
     *
     * @param cursor The cursor returned from a database query.
     * @return A new {@link com.recipe_app.client.Recipe} object with the basic attributes populated.
     */
    public static Recipe fromCursor(Cursor cursor) {
        Recipe recipe = new Recipe(null);
        for (int c=0; c<cursor.getColumnCount(); c++) {
            String columnName = cursor.getColumnName(c);
            if (columnName.equals(RecipeTable.ID_COLUMN)) {
                recipe.id = cursor.getString(c);
            } else if (columnName.equals(RecipeTable.TITLE_COLUMN)) {
                recipe.setTitle(cursor.getString(c));
            } else if (columnName.equals(RecipeTable.DESCRIPTION_COLUMN)) {
                recipe.setDescription(cursor.getString(c));
            } else if (columnName.equals(RecipeTable.PHOTO_COLUMN)) {
                recipe.setPhoto(cursor.getString(c));
            } else if (columnName.equals(RecipeTable.PREP_TIME_COLUMN)) {
                recipe.setPrepTime(cursor.getString(c));
            }
        }
        return recipe;
    }

    public static class Ingredient {
        private String amount;
        private String description;

        public String getAmount() {
            return amount;
        }

        public void setAmount(String amount) {
            this.amount = amount;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        /**
         * Static helper method for populating attributes from a database cursor.
         *
         * @param cursor The cursor returned from a database query.
         * @return A new {@link com.recipe_app.client.Recipe.Ingredient} object with all attributes populated.
         */
        public static Ingredient fromCursor(Cursor cursor) {
            Ingredient ingredient = new Ingredient();
            for (int c=0; c<cursor.getColumnCount(); c++) {
                String columnName = cursor.getColumnName(c);
                if (columnName.equals(RecipeIngredientTable.AMOUNT_COLUMN)) {
                    ingredient.setAmount(cursor.getString(c));
                } else if (columnName.equals(RecipeIngredientTable.DESCRIPTION_COLUMN)) {
                    ingredient.setDescription(cursor.getString(c));
                }
            }
            return ingredient;
        }
    }

    public static class Step {
        private String description;
        private String photo;

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getPhoto() {
            return photo;
        }

        public void setPhoto(String photo) {
            this.photo = photo;
        }

        /**
         * Static helper method for populating attributes from a database cursor.
         *
         * @param cursor The cursor returned from a database query.
         * @return A new {@link com.recipe_app.client.Recipe.Step} object with all attributes populated.
         */
        public static Step fromCursor(Cursor cursor) {
            Step step = new Step();
            for (int c=0; c<cursor.getColumnCount(); c++) {
                String columnName = cursor.getColumnName(c);
                if (columnName.equals(RecipeInstructionsTable.PHOTO_COLUMN)) {
                    step.setPhoto(cursor.getString(c));
                } else if (columnName.equals(RecipeInstructionsTable.DESCRIPTION_COLUMN)) {
                    step.setDescription(cursor.getString(c));
                }
            }
            return step;
        }
    }

}