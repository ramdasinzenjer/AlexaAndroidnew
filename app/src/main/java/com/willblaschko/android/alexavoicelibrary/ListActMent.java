package com.willblaschko.android.alexavoicelibrary;

import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.TextView;

import com.willblaschko.android.alexavoicelibrary.actions.logout;

public class ListActMent extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
      /*  requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setBackgroundDrawable(
                new ColorDrawable(android.graphics.Color.TRANSPARENT));*/
        setContentView(R.layout.activity_list);

        TextView skills = (TextView) findViewById(R.id.skills);
        skills.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                alexa_skills();
            }
        });

    }
    void alexa_skills()
    {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://developer.amazon.com/edw/home.html#/skills"));
        startActivity(browserIntent);
    }
    void rate(View v)
    {

    }

}
