package com.willblaschko.android.alexavoicelibrary;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

/**
 * Our main launch activity where we can change settings, see about, etc.
 */
public class MainActivity extends AppCompatActivity {
    /*
    Currently empty because we don't really have anything to put in here,
    this would be where we show our normal launch activity which can be anything
     */
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //nothing to see here, bye bye!
        this.finish();
    }
}
