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


/**
 * Created by simister on 10/21/14.
 */
public class RecipeTable {

    // Database table
    public static final String TABLE = "recipes";
    public static final String ID_COLUMN = "_id";
    public static final String ID = TABLE + "." + ID_COLUMN;
    public static final String TITLE_COLUMN = "title";
    public static final String TITLE = TABLE + "." + TITLE_COLUMN;
    public static final String PHOTO_COLUMN = "photo";
    public static final String PHOTO = TABLE + "." + PHOTO_COLUMN;
    public static final String PREP_TIME_COLUMN = "prep_time";
    public static final String PREP_TIME = TABLE + "." + PREP_TIME_COLUMN;
    public static final String DESCRIPTION_COLUMN = "description";
    public static final String DESCRIPTION = TABLE + "." + DESCRIPTION_COLUMN;
}
