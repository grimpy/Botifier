package com.github.grimpy.botifier.plugins.liveware;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * The extension receiver receives the extension intents and starts the
 * extension service when it arrives.
 */
public class SWExtensionReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(final Context context, final Intent intent) {
        intent.setClass(context, SWExtensionService.class);
        context.startService(intent);
    }
}
