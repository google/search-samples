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

package com.recipe_app.client.database;

public class RecipeNoteTable {
    public static final String TABLE = "recipe_notes";
    public static final String ID_COLUMN = "_id";
    public static final String ID = TABLE + "." + ID_COLUMN;
    public static final String RECIPE_ID_COLUMN ="recipe_id";
    public static final String RECIPE_ID = TABLE + "." + RECIPE_ID_COLUMN;
    public static final String TEXT_COLUMN = "text";
    public static final String TEXT = TABLE + "." + TEXT_COLUMN;
}
