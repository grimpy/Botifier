package com.github.grimpy.botifier.plugins.avrcp;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.media.MediaMetadata;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;

import com.github.grimpy.botifier.Botification;
import com.github.grimpy.botifier.R;
import com.github.grimpy.botifier.plugins.AbstractPlugin;

import java.util.ArrayList;


public class AvrcpService5 extends AbstractPlugin {

    public static final String SERVICECMD = "com.github.grimpy.botifier.cmd";

	private static String TAG = "Botifier";
	private SharedPreferences mSharedPref;
    private MediaSession mMediaSession;
    private AudioManager mAudioManager;
	private int HANDLER_WHAT_CLEAR = 1;

	private ArrayList<Botification> mNotifications;


    private int mCurrent = -1;
	private int mAudiofocus = -1;
	private Handler mHandler;

    @Override
    public void onCreate() {
        super.onCreate();
        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        mMediaSession = new MediaSession(this, TAG);
        setUpCallBack();
        mNotifications = new ArrayList<Botification>();

        final IntentFilter filter = new IntentFilter();
        filter.addAction(SERVICECMD);
        // Attach the broadcast listener
        mSharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        mHandler = new Handler(){
            public void handleMessage(Message msg){
                resetNotify(true);
            }
        };
    }

    private void setUpCallBack() {
        mMediaSession.setFlags(MediaSession.FLAG_HANDLES_MEDIA_BUTTONS |
                MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS);
        mMediaSession.setCallback(new MediaSession.Callback() {
            @Override
            public void onPlay() {
                super.onPlay();
                removeNotification();
            }

            @Override
            public void onPause() {
                super.onPause();
                removeNotification();
            }

            @Override
            public void onSkipToNext() {
                super.onSkipToNext();
                showNotify(1);
            }

            @Override
            public void onSkipToPrevious() {
                super.onSkipToPrevious();
            }

            @Override
            public void onStop() {
                super.onStop();
                resetNotify(true);
                showNotify(-1);
            }
        });
    }

    private boolean isActive() {
    	return mAudioManager.isBluetoothA2dpOn() || !mSharedPref.getBoolean(_(R.string.pref_metadata_bt_only), true);
    }
    
    private void removeNotification() {
    	if (mCurrent == -1 || mCurrent > mNotifications.size() -1) {
    		resetNotify(true);
    		return;
    	}
    	Log.d(TAG, "Remove current notification: " + mCurrent);
    	Botification old = mNotifications.get(mCurrent);
    	removeNotification(old);
    } 	
    private void removeNotification(Botification old) {
        super.removeNotifcation(old);
        for (Botification bot: mNotifications) {
            if (bot.equals(old)){
                Log.d(TAG, "Notification found and remove");
                mNotifications.remove(bot);
                break;
            }
        }
    	if (mNotifications.size() == 0) {
    		mCurrent = -1;
    		resetNotify(true);
    		return;
    	}
    	showNotify(0, true);
    }
    
	private int getTimeout() {
		String timeout = mSharedPref.getString(_(R.string.pref_timeout), "");
		if (!TextUtils.isEmpty(timeout)){
			return Integer.valueOf(timeout);
		}
		return 0;
	}
    
    private void showNotify(int offset){
    	showNotify(offset, false);
    }
    
    private void showNotify(int offset, boolean next) {
    	if (mNotifications.size() == 0) {
    		mCurrent = -1;
    		return;
    	}
    	int idx = mCurrent + offset;   	
    	if (idx >= mNotifications.size()) {
    		idx = 0;
    	} else if (idx < 0) {
    		idx = mNotifications.size() -1;
    	}
    	if (mCurrent >= mNotifications.size()){
    		mCurrent = mNotifications.size() -1;
    	}
    	Botification current = mNotifications.get(mCurrent);
    	if (next || ( offset > 0 && mCurrent != -1 && !current.hasNext())) {
    		showNotify(mNotifications.get(idx));
    	} else {
    		showNotify(current);
    	}
    	
    }
    
    private void resetNotify(boolean close) {
        if (close) {
        	if (mMediaSession != null) {
                mMediaSession.setActive(false);
        	}
        } else {
            showNotify("Botifier", "Botifier", "Botifier", 0);
        }
    }




	public void showNotify(Botification notify) {
		Log.d(TAG, "Setting notification " + notify.toString());
		mCurrent = mNotifications.indexOf(notify);
		if (isActive()) {
			Log.d(TAG, "Setting Metadata");
	        showNotify(notify.getPreference(_(R.string.pref_metadata_artist)), notify.getPreference(_(R.string.pref_metadata_album)), notify.getPreference(_(R.string.pref_metadata_title)), 1);
		}
	}
	
	public void showNotify(String artist, String album, String title, int tracknr) {
        PlaybackState state = new PlaybackState.Builder()
                .setActions(
                        PlaybackState.ACTION_PLAY | PlaybackState.ACTION_PLAY_PAUSE |
                                PlaybackState.ACTION_PLAY_FROM_MEDIA_ID | PlaybackState.ACTION_PAUSE |
                                PlaybackState.ACTION_SKIP_TO_NEXT | PlaybackState.ACTION_SKIP_TO_PREVIOUS)
                .setState(PlaybackState.STATE_PLAYING, 1, 1, SystemClock.elapsedRealtime())
                .build();
        MediaMetadata metadata = new MediaMetadata.Builder()
                .putString(MediaMetadata.METADATA_KEY_TITLE, title)
                .putString(MediaMetadata.METADATA_KEY_ARTIST, artist)
                .putString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST, artist)
                .putString(MediaMetadata.METADATA_KEY_ALBUM, album)
                .putLong(MediaMetadata.METADATA_KEY_NUM_TRACKS, tracknr)
                .putLong(MediaMetadata.METADATA_KEY_DURATION, 10)
                .build();
        mMediaSession.setActive(true);
        mMediaSession.setMetadata(metadata);
        mMediaSession.setPlaybackState(state);
		int timeout = getTimeout();
		if (timeout != 0) {
			mHandler.removeMessages(HANDLER_WHAT_CLEAR);
			mHandler.sendEmptyMessageDelayed(HANDLER_WHAT_CLEAR, timeout * 1000);
		}
	}
	
    private final OnAudioFocusChangeListener mAudioFocusListener = new OnAudioFocusChangeListener() {
        /**
         * {@inheritDoc}
         */
        @Override
        public void onAudioFocusChange(final int focusChange) {
            Log.d(TAG, "Focus change request " + focusChange);
            mAudiofocus = focusChange;
        }
    };
    
	private void addNotification(Botification notification) {
		for (int i = 0; i < mNotifications.size(); i++) {
			Botification not = mNotifications.get(i);
			Log.d(TAG, "Adding notification comparing with " + not.mPkg);
			if (not.equals(notification)) {
				mNotifications.set(i, notification);
				return;
			}
		}
		mNotifications.add(notification);
	}
	
	public void destroy() {
		Log.d(TAG, "AvrcpService interrupted");
		mAudiofocus = -1;
		mAudioManager.abandonAudioFocus(mAudioFocusListener);
	}
	
	public void notificationAdded(Botification notification) {
        addNotification(notification);
        showNotify(notification);
	}

    @Override
    public void notificationRemoved(Botification not) {
        removeNotification(not);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

}