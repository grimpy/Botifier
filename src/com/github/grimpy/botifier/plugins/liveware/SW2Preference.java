package com.github.grimpy.botifier.plugins.liveware;

import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;

import com.github.grimpy.botifier.R;

public class SW2Preference extends PreferenceFragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.sw2_preferences);
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        super.onPreferenceTreeClick(preferenceScreen, preference);
        if (!isAdded()) {
            return false;
        }
        String prefkey = preference.getKey();
        if (prefkey != null) {
            if (prefkey.equals(getString(R.string.pref_sw_clear))) {
                Intent clearint = new Intent(getActivity(), SWExtensionService.class);
                clearint.setAction(SWExtensionService.INTENT_ACTION_CLEAR);
                getActivity().startService(clearint);
            }
        }
        return true;
    }

}
