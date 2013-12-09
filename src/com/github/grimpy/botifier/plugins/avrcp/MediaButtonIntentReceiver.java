/*
 * Copyright (C) 2007 The Android Open Source Project Licensed under the Apache
 * License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law
 * or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

package com.github.grimpy.botifier.plugins.avrcp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;

import com.github.grimpy.botifier.plugins.avrcp.AvrcpService;

/**
 * Used to control headset playback. Single press: pause/resume. Double press:
 * next track Long press: voice search.
 */
public class MediaButtonIntentReceiver extends BroadcastReceiver {

    private static final int MSG_LONGPRESS_TIMEOUT = 1;

    private static final int LONG_PRESS_DELAY = 1000;

    private static final int DOUBLE_CLICK = 800;

    private static long mLastClickTime = 0;

    private static boolean mDown = false;

    private static boolean mLaunched = false;
    private static String TAG = "Botifier";

    private static Handler mHandler = new Handler() {

        /**
         * {@inheritDoc}
         */
        @Override
        public void handleMessage(final Message msg) {
            switch (msg.what) {
                case MSG_LONGPRESS_TIMEOUT:
                    if (!mLaunched) {
                    	//TODO: something with long press
                    }
                    break;
            }
        }
    };

    /**
     * {@inheritDoc}
     */
    @Override
    public void onReceive(final Context context, final Intent intent) {
    	final String intentAction = intent.getAction();
        if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(intentAction)) {
        	//Bluetooth disconnected or headset diconnected
        } else if (Intent.ACTION_MEDIA_BUTTON.equals(intentAction)) {
            final KeyEvent event = (KeyEvent)intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
            if (event == null) {
                return;
            }

            final int keycode = event.getKeyCode();
            final int action = event.getAction();
            final long eventtime = event.getEventTime();
            Log.d(TAG, String.format("Key pressed action=%d keycode=%d eventtime=%d", action, keycode, eventtime));

            if (KeyEvent.ACTION_UP == action) {
	            final Intent i = new Intent();
	            i.setAction(AvrcpService.SERVICECMD);
	            i.putExtra(AvrcpService.SERVICECMD, keycode);
	            context.sendBroadcast(i);
            }
            
        }
    }
}
