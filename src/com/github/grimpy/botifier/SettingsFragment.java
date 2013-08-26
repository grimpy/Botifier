package com.github.grimpy.botifier;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;

import com.github.grimpy.botifier.R;

public class SettingsFragment extends PreferenceFragment implements OnSharedPreferenceChangeListener{
	private SharedPreferences mSharedPref;
		
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		addPreferencesFromResource(R.xml.botifier_preference);
		mSharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity());
		mSharedPref.registerOnSharedPreferenceChangeListener(this);
		String[] prefs = {"metadata_artist", "metadata_album", "metadata_title"};
		for (String prefkey : prefs) {
			String value = mSharedPref.getString(prefkey, "");
			Preference pref = findPreference(prefkey);
			pref.setSummary(value);
		}
		
	}
	
	public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
		String prefkey = preference.getKey();
		if (prefkey != null) { 
			if (prefkey.equals("action_reload") ) {
	            final Intent i = new Intent();
	            i.setAction(BotifierService.NOTIFICATION);
	            i.putExtra("album", "Botifier");
	            i.putExtra("artist", "Botifier");
	            i.putExtra("title", "Botifier");
	            getActivity().sendBroadcast(i);
			} else if (prefkey.equals("blacklist")) {
				 getFragmentManager().beginTransaction()
	                .replace(android.R.id.content, new BlackListFragment())
	                .commit();
			}
		}
		return true;
	}

		
	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
		if (key.equals("action_tts") || key.equals("blacklistentries")) {
			return;
		}
		String msg = sharedPreferences.getString(key, "");
		Preference pref = findPreference(key);
		pref.setSummary(msg);
		if (key.equals("action_message")){
			final Intent i = new Intent();
            i.setAction(BotifierService.NOTIFICATION);
            i.putExtra("album", msg);
            i.putExtra("artist", msg);
            i.putExtra("title", msg);
            getActivity().sendBroadcast(i);
		}
		// TODO Auto-generated method stub
		
	}
    
    
}
