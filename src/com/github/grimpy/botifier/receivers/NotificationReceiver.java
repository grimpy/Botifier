package com.github.grimpy.botifier.receivers;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;

import com.github.grimpy.botifier.R;
import com.github.grimpy.botifier.plugins.avrcp.AvrcpService;
import com.github.grimpy.botifier.plugins.avrcp.AvrcpService5;
import com.github.grimpy.botifier.plugins.tts.TTSService;

import com.github.grimpy.botifier.plugins.liveware.SWExtensionService;

/**
 * The extension receiver receives the extension intents and starts the
 * extension service when it arrives.
 */
public class NotificationReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(final Context context, final Intent intent) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        if (prefs.getBoolean(context.getString(R.string.pref_tts_enabled), false)) {
            Intent tts = new Intent(intent);
            tts.setClass(context, TTSService.class);
            context.startService(tts);
        }
        if (prefs.getBoolean(context.getString(R.string.pref_sw_enable), false)) {
            Intent sw = new Intent(intent);
            sw.setClass(context, SWExtensionService.class);
            context.startService(sw);
        }
        if (prefs.getBoolean(context.getString(R.string.pref_avrcp_enable), false)) {
            Intent avrcp = new Intent(intent);
            if (Build.VERSION.SDK_INT >= 21) { // Lollipop 5.0
                avrcp.setClass(context, AvrcpService5.class);
            } else {
                avrcp.setClass(context, AvrcpService.class);
            }
            context.startService(avrcp);
        }
    }
}
