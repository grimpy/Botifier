package com.github.grimpy.botifier.plugins;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.util.Log;

import com.github.grimpy.botifier.R;
import com.github.grimpy.botifier.plugins.avrcp.AVRCPPreference;
import com.github.grimpy.botifier.preference.AbstractPreferenceFragment;
import com.github.grimpy.botifier.preference.PreferenceSwitch;
import com.github.grimpy.botifier.plugins.liveware.SW2Preference;
import com.github.grimpy.botifier.plugins.tts.TTSPreference;

public class PluginPreference extends AbstractPreferenceFragment {

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.plugins_preferences);
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        super.onPreferenceTreeClick(preferenceScreen, preference);
        if (!isAdded()) {
            return false;
        }
        String prefkey = preference.getKey();
        if (prefkey != null) {
            Log.d("Botifier", "show open ");
            PreferenceSwitch mypref = (PreferenceSwitch) preference;
            if (prefkey.equals(getString(R.string.pref_avrcp_enable))) {
                getFragmentManager().beginTransaction().replace(android.R.id.content, new AVRCPPreference()).addToBackStack(null)
                        .commit();
            } else if (prefkey.equals(getString(R.string.pref_tts_enabled))) {
                getFragmentManager().beginTransaction().replace(android.R.id.content, new TTSPreference()).addToBackStack(null)
                        .commit();
            } else if (prefkey.equals(getString(R.string.pref_sw_enable))) {
                getFragmentManager().beginTransaction().replace(android.R.id.content, new SW2Preference()).addToBackStack(null)
                        .commit();
            }
        }

        return true;
    }
}
