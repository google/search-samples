package com.recipe_app.client.database;

/**
 * Created by simister on 10/22/14.
 */
public class RecipeIngredientTable {
    public static final String TABLE = "recipe_ingredients";
    public static final String ID = TABLE + "._id";
    public static final String RECIPE_ID = TABLE + ".recipe_id";
    public static final String AMOUNT = TABLE + ".amount";
    public static final String DESCRIPTION = TABLE + ".description";
}
