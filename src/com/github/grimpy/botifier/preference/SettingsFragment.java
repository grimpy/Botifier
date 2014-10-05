package com.github.grimpy.botifier.preference;

import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.support.v4.app.NotificationCompat;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;

import com.github.grimpy.botifier.R;
import com.github.grimpy.botifier.plugins.PluginPreference;
import com.github.grimpy.botifier.plugins.liveware.SWExtensionService;

public class SettingsFragment extends AbstractPreferenceFragment implements OnSharedPreferenceChangeListener{

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.botifier_preference);
    }


    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        super.onPreferenceTreeClick(preferenceScreen, preference);
        if (!isAdded()) {
            return false;
        }
        String prefkey = preference.getKey();
        if (prefkey != null) {

            if (prefkey.equals(getString(R.string.action_makenotification)) ) {
                NotificationManager nManager = (NotificationManager) getActivity().getSystemService(Context.NOTIFICATION_SERVICE);
                NotificationCompat.Builder ncomp = new NotificationCompat.Builder(getActivity());
                ncomp.setContentTitle("My Notification");
                ncomp.setContentText(String.format("%s", new java.util.Date().getSeconds()));
                ncomp.setTicker("Botifier ticker test");
                ncomp.setSmallIcon(R.drawable.ic_launcher);
                ncomp.setAutoCancel(true);
                Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                ncomp.setSound(alarmSound);
                nManager.notify((int)System.currentTimeMillis(),ncomp.build());
            } else if (prefkey.equals(getString(R.string.action_blacklist))) {
                getFragmentManager().beginTransaction()
                        .replace(android.R.id.content, new BlackListFragment()).addToBackStack(null)
                        .commit();
            } else if (prefkey.equals(getString(R.string.action_filter_applications))) {
                getFragmentManager().beginTransaction()
                        .replace(android.R.id.content, new ApplicationFilterFragment()).addToBackStack(null)
                        .commit();
            } else if (prefkey.equals(getString(R.string.pref_sw_clear))) {
                    Intent clearint = new Intent(getActivity(), SWExtensionService.class);
                    clearint.setAction(SWExtensionService.INTENT_ACTION_CLEAR);
                    getActivity().startService(clearint);
            } else if (prefkey.equals(getString(R.string.pref_plugins))) {
                getFragmentManager().beginTransaction()
                        .replace(android.R.id.content, new PluginPreference()).addToBackStack(null)
                        .commit();
            } else if (prefkey.equals(getString(R.string.pref_open_access))) {
                String action;
                if (Build.VERSION.SDK_INT >= 18) {
                    action = "android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS";
                } else {
                    action = Settings.ACTION_ACCESSIBILITY_SETTINGS;
                }
                startActivity(new Intent(action));
            }
        }

        return true;
    }



    @Override
    public void onSharedPreferenceChanged(final SharedPreferences sharedPreferences,
                                          final String key) {
        if (key.equals(getString(R.string.pref_blacklist)) || key.equals(getString(R.string.pref_tts_bt_only))) {
            return;
        }
    }
}
