package com.github.grimpy.botifier.plugins.avrcp;

import java.util.ArrayList;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.media.RemoteControlClient;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.media.RemoteControlClient.MetadataEditor;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;

import com.github.grimpy.botifier.plugins.AbstractPlugin;
import com.github.grimpy.botifier.Botification;
import com.github.grimpy.botifier.R;


public class AvrcpService extends AbstractPlugin {

    public static final String SERVICECMD = "com.github.grimpy.botifier.cmd";

	private static String TAG = "Botifier";
	private SharedPreferences mSharedPref;
    private TelephonyManager mTelephonyManager;
    private AudioManager mAudioManager;
	private RemoteControlClient mRemoteControlClient;
	private int HANDLER_WHAT_CLEAR = 1;

	private ComponentName mMediaButtonReceiverComponent;
	private ArrayList<Botification> mNotifications;


    private int mCurrent = -1;
	private int mAudiofocus = -1;
	private Handler mHandler;

    @Override
    public void onCreate() {
        super.onCreate();
        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        mMediaButtonReceiverComponent = new ComponentName(getPackageName(), MediaButtonIntentReceiver.class.getName());
        mAudioManager.registerMediaButtonEventReceiver(mMediaButtonReceiverComponent);
        mNotifications = new ArrayList<Botification>();

        final IntentFilter filter = new IntentFilter();
        filter.addAction(SERVICECMD);
        // Attach the broadcast listener
        registerReceiver(mIntentReceiver, filter);
        mSharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        mHandler = new Handler(){
            public void handleMessage(Message msg){
                resetNotify(true);
            }
        };
    }

    private final BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        /**
         * {@inheritDoc}
         */
        @Override
        public void onReceive(final Context context, final Intent intent) {
        	Log.d(TAG, "Received action " + intent.getAction());
        	if (intent.getAction().equals(SERVICECMD) ) {
	        	int keycode = intent.getIntExtra(SERVICECMD, 0);
	        	Log.d(TAG, "Recieved key" + keycode);
	            switch (keycode) {
	            	case KeyEvent.KEYCODE_MEDIA_FAST_FORWARD:
	            		resetNotify(true);
	            		break;
		            case KeyEvent.KEYCODE_MEDIA_STOP:
		            case KeyEvent.KEYCODE_HEADSETHOOK:
		            case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
		            case KeyEvent.KEYCODE_MEDIA_PLAY:
		            case KeyEvent.KEYCODE_MEDIA_PAUSE:
		            	removeNotification();
		                break;
		            case KeyEvent.KEYCODE_MEDIA_NEXT:
		            	showNotify(1);
		                break;
		            case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
		            	showNotify(-1);
		                break;
	            }
        	}
        }
    };

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
    
    private void getAudioFocus() {
    	if (true || mAudiofocus != AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK) {
    		setUpRemoteControlClient();
    		Log.d(TAG, "Focus acquire " + mAudiofocus);
    	}
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
        	if (mRemoteControlClient != null) {
        		mRemoteControlClient.setPlaybackState(RemoteControlClient.PLAYSTATE_STOPPED);
        		mAudioManager.unregisterRemoteControlClient(mRemoteControlClient);
        	}
        	mAudioManager.abandonAudioFocus(mAudioFocusListener);
            mAudioManager.unregisterMediaButtonEventReceiver(mMediaButtonReceiverComponent);
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
        getAudioFocus();
        mRemoteControlClient.setPlaybackState(RemoteControlClient.PLAYSTATE_PLAYING);
		MetadataEditor edit = mRemoteControlClient.editMetadata(true);
		edit.putString(MediaMetadataRetriever.METADATA_KEY_TITLE, title);
		edit.putString(MediaMetadataRetriever.METADATA_KEY_ARTIST, artist);
		edit.putString(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST, artist);
		edit.putString(MediaMetadataRetriever.METADATA_KEY_ALBUM, album);
		edit.putLong(MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER, tracknr);
        edit.putLong(MediaMetadataRetriever.METADATA_KEY_DURATION, 10);
		edit.apply();
		int timeout = getTimeout();
		if (timeout != 0) {
			mHandler.removeMessages(HANDLER_WHAT_CLEAR);
			mHandler.sendEmptyMessageDelayed(HANDLER_WHAT_CLEAR, timeout * 1000);
		}
	}
	
    private void setUpRemoteControlClient() {
        mMediaButtonReceiverComponent = new ComponentName(getPackageName(), MediaButtonIntentReceiver.class.getName());
    	mAudioManager.registerMediaButtonEventReceiver(mMediaButtonReceiverComponent);
        final Intent mediaButtonIntent = new Intent(Intent.ACTION_MEDIA_BUTTON);
        mediaButtonIntent.setComponent(mMediaButtonReceiverComponent);
        mRemoteControlClient = new RemoteControlClient(
                PendingIntent.getBroadcast(getApplicationContext(), 0, mediaButtonIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT));
        mAudioManager.registerRemoteControlClient(mRemoteControlClient);

        // Flags for the media transport control that this client supports.
        final int flags = RemoteControlClient.FLAG_KEY_MEDIA_PREVIOUS
                | RemoteControlClient.FLAG_KEY_MEDIA_NEXT
                | RemoteControlClient.FLAG_KEY_MEDIA_PLAY
                | RemoteControlClient.FLAG_KEY_MEDIA_PAUSE
                | RemoteControlClient.FLAG_KEY_MEDIA_PLAY_PAUSE
                | RemoteControlClient.FLAG_KEY_MEDIA_STOP;
        mRemoteControlClient.setTransportControlFlags(flags);
        mAudiofocus = mAudioManager.requestAudioFocus(mAudioFocusListener,
                AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK);

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