package com.github.grimpy.botifier;

import android.app.Activity;
import android.os.Bundle;

import com.github.grimpy.botifier.preference.SettingsFragment;

public class MainActivity extends Activity {

	protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Display the fragment as the main content.
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new SettingsFragment())
                .commit();
    }
}