package com.github.grimpy.botifier.plugins.avrcp;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.text.InputType;
import android.widget.EditText;

import java.util.List;
import java.util.Map;
import java.util.Arrays;
import java.util.HashMap;

import com.github.grimpy.botifier.R;
import com.github.grimpy.botifier.preference.AbstractPreferenceFragment;

public class AVRCPPreference extends AbstractPreferenceFragment {

    private static String[] META = {"metadata_artist", "metadata_album", "metadata_title", "tts_value"};

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.avrcp_preferences);

        mFields = Arrays.asList(getResources().getStringArray(R.array.metadata_fields));
        mValues = Arrays.asList(getResources().getStringArray(R.array.metadata_fields_values));
        for (String prefkey : META) {
            String value = mSharedPref.getString(prefkey, "");
            setSummary(prefkey, value);
        }
    }

    @Override
    public void onSharedPreferenceChanged(final SharedPreferences sharedPreferences,
                                          final String key) {
        if (Arrays.asList(META).contains(key)) {
            String msg = sharedPreferences.getString(key, "");
            if (msg.equals("custom")) {
                final EditText input = new EditText(getActivity());
                // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
                input.setInputType(InputType.TYPE_CLASS_TEXT);
                input.setText(mPrefCache.get(key));
                input.selectAll();
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setView(input);
                builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String setting = input.getText().toString();
                        sharedPreferences.edit().putString(key, setting).apply();
                    }
                });
                builder.setTitle(R.string.custom_title);
                builder.setMessage(R.string.custom_description);
                AlertDialog dialog = builder.create();
                dialog.show();
            } else {
                setSummary(key, msg);
            }

        }

    }
}
