package com.willblaschko.android.alexavoicelibrary;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.willblaschko.android.alexavoicelibrary.actions.BaseListenerFragment;

/**
 * Our main launch activity where we can change settings, see about, etc.
 */
public class MainActivity extends BaseActivity implements ActionsFragment.ActionFragmentInterface {
    private final static String TAG = "MainActivity";
    private final static String TAG_FRAGMENT = "CurrentFragment";

    private View statusBar;
    private TextView status;
    private View loading;

    private FrameLayout frame;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        frame = (FrameLayout) findViewById(R.id.frame);

        statusBar = findViewById(R.id.status_bar);
        status = (TextView) findViewById(R.id.status);
        loading = findViewById(R.id.loading);

        ActionsFragment fragment = new ActionsFragment();
        loadFragment(fragment, false);
    }

    @Override
    protected void startListening() {
        Fragment fragment = getSupportFragmentManager().findFragmentByTag(TAG_FRAGMENT);
        if (fragment != null && fragment.isVisible()) {
            // add your code here
            if(fragment instanceof BaseListenerFragment){
                ((BaseListenerFragment) fragment).startListening();
            }
        }
    }

    @Override
    public void loadFragment(Fragment fragment, boolean addToBackStack){
        FragmentTransaction transaction = getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.frame, fragment, TAG_FRAGMENT);
        if(addToBackStack){
            transaction.addToBackStack(fragment.getClass().getSimpleName());
        }
        transaction.commit();
    }


    protected void stateListening(){
        status.setText(R.string.status_listening);
        loading.setVisibility(View.GONE);
        statusBar.animate().alpha(1);
    }
    protected void stateProcessing(){
        status.setText(R.string.status_processing);
        loading.setVisibility(View.VISIBLE);
        statusBar.animate().alpha(1);
    }
    protected void stateSpeaking(){
        status.setText(R.string.status_speaking);
        loading.setVisibility(View.VISIBLE);
        statusBar.animate().alpha(1);
    }
    protected void statePrompting(){
        status.setText("");
        loading.setVisibility(View.VISIBLE);
        statusBar.animate().alpha(1);
    }
    protected void stateFinished(){
        status.setText("");
        loading.setVisibility(View.GONE);
        statusBar.animate().alpha(0);
    }
    protected void stateNone(){
        statusBar.animate().alpha(0);
    }
}
