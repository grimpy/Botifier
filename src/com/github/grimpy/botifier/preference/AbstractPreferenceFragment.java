package com.github.grimpy.botifier.preference;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Jo on 12/13/13.
 */
abstract public class AbstractPreferenceFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {
    protected SharedPreferences mSharedPref;
    protected Map<String,String> mPrefCache = new HashMap<String,String>();
    protected List<String> mFields;
    protected List<String> mValues;

    protected void setSummary(String prefkey, String value) {
        Preference pref = findPreference(prefkey);
        mPrefCache.put(prefkey, value);
        if (mValues != null) {
            int idx = mValues.indexOf(value);
            if (idx >= 0) {
                value = mFields.get(idx);
            }
        }
        if (pref != null) {
            pref.setSummary(value.replace("%", "%%"));
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mSharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity());
        mSharedPref.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (mSharedPref != null) {
            mSharedPref.registerOnSharedPreferenceChangeListener(this);
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mSharedPref.unregisterOnSharedPreferenceChangeListener(this);
    }
}
