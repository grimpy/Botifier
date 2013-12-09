package com.github.grimpy.botifier.plugins.tts;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;

import com.github.grimpy.botifier.R;
import com.github.grimpy.botifier.preference.AbstractPreferenceFragment;

public class TTSPreference extends AbstractPreferenceFragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTTSVisiblity();
        addPreferencesFromResource(R.xml.tts_preferences);
    }

    private void setTTSVisiblity() {
        boolean tts = mSharedPref.getBoolean(getString(R.string.pref_tts_enabled), false);
        Preference pref = findPreference(getString(R.string.pref_tts_value));
        if (pref != null)
            pref.setEnabled(tts);
        pref = findPreference(getString(R.string.pref_tts_bt_only));
        if (pref != null)
            pref.setEnabled(tts);
    }

    @Override
    public void onSharedPreferenceChanged(final SharedPreferences sharedPreferences,
                                          final String key) {
        if (key.equals(getString(R.string.pref_tts_enabled))) {
            setTTSVisiblity();
            return;
        }
    }
}
