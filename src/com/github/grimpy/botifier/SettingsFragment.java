package com.github.grimpy.botifier;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.support.v4.app.NotificationCompat;
import android.text.InputType;
import android.widget.EditText;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
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
			pref.setSummary(value.replace("%", "%%"));
		}
		
	}
	
	public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
		String prefkey = preference.getKey();
		if (prefkey != null) {
			
			if (prefkey.equals("action_makenotification") ) {
	            NotificationManager nManager = (NotificationManager) getActivity().getSystemService(Context.NOTIFICATION_SERVICE);
	            NotificationCompat.Builder ncomp = new NotificationCompat.Builder(getActivity());
	            ncomp.setContentTitle("My Notification");
	            ncomp.setContentText(String.format("%s", new java.util.Date().getSeconds()));
	            ncomp.setTicker("Botifier ticker test");
	            ncomp.setSmallIcon(R.drawable.ic_launcher);
	            ncomp.setAutoCancel(true);
	            nManager.notify((int)System.currentTimeMillis(),ncomp.build());

			} else if (prefkey.equals("blacklist")) {
				 getFragmentManager().beginTransaction()
	                .replace(android.R.id.content, new BlackListFragment()).addToBackStack(null)
	                .commit();
			}
		}
		return true;
	}

		
	@Override
	public void onSharedPreferenceChanged(final SharedPreferences sharedPreferences,
			final String key) {
		if (key.equals("action_tts") || key.equals("blacklistentries")) {
			return;
		}
		String msg = sharedPreferences.getString(key, "");
		if (key.startsWith("metadata_") && msg.equals("custom")) {
			final EditText input = new EditText(getActivity());
			// Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
			input.setInputType(InputType.TYPE_CLASS_TEXT);
			AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
			builder.setView(input);
			builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() { 
			    @Override
			    public void onClick(DialogInterface dialog, int which) {
			        String setting = input.getText().toString();
			        sharedPreferences.edit().putString(key, setting).apply();
			    }
			});
			builder.setTitle(R.string.custom_title);
			AlertDialog dialog = builder.create();
			dialog.show();
			
		}
		Preference pref = findPreference(key);
		pref.setSummary(msg.replace("%", "%%"));
	}
    
    
}
