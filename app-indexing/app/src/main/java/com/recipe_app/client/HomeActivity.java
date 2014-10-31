package com.recipe_app.client;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;

import com.recipe_app.R;

/**
 * Created by simister on 10/24/14.
 */
public class HomeActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        findViewById(R.id.splash_bg).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(view.getContext(), RecipeActivity.class);
                intent.setAction(Intent.ACTION_VIEW);
                intent.setData(Uri.parse("http://recipe-app.com/recipes/grilled-potato-salad"));
                startActivity(intent);
            }
        });
    }
}
