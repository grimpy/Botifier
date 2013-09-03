package com.github.grimpy.botifier;

import java.util.ArrayList;
import java.util.Set;
import java.util.regex.Pattern;

import android.app.PendingIntent;
import android.app.Service;
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
import android.preference.PreferenceManager;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.util.Log;
import android.view.KeyEvent;


public class BotifierManager implements OnInitListener {

	public static final String SERVICECMD = "com.github.grimpy.botifier.cmd";
	public static final String NOTIFICATION = "com.github.grimpy.botifier.notification";
	public static final String CMD_NOTIFICATION_ADDED = "com.github.grimpy.botifier.notification.added";
	public static final String CMD_NOTIFICATION_REMOVED = "com.github.grimpy.botifier.notification.removed"; 
	
	private boolean isInit = false;
	private static String TAG = "Botifier";
	private SharedPreferences mSharedPref;
	private RemoteControlClient mRemoteControlClient;
	private AudioManager mAudioManager;
	private ComponentName mMediaButtonReceiverComponent;
	private ArrayList<Botification> mNotifications;
	private int mCurrent = -1;
	private int mAudiofocus = -1;
	private TextToSpeech mTTS;
	private Service mService;

    public BotifierManager(Service serv) {
    	mService = serv;
	    if (isInit) {
	        return;
	    }
        mAudioManager = (AudioManager) mService.getSystemService(Context.AUDIO_SERVICE);
        mMediaButtonReceiverComponent = new ComponentName(mService.getPackageName(), MediaButtonIntentReceiver.class.getName());
    	mAudioManager.registerMediaButtonEventReceiver(mMediaButtonReceiverComponent);
		// build the PendingIntent for the remote control client
		//setUpRemoteControlClient();
		
		mNotifications = new ArrayList<Botification>();
		mTTS = new TextToSpeech(mService, this);
		
        final IntentFilter filter = new IntentFilter();
        filter.addAction(SERVICECMD);
        filter.addAction(NOTIFICATION);
        filter.addAction(CMD_NOTIFICATION_ADDED);
        filter.addAction(CMD_NOTIFICATION_REMOVED);
        // Attach the broadcast listener
        mService.registerReceiver(mIntentReceiver, filter);
        mSharedPref = PreferenceManager.getDefaultSharedPreferences(mService);
	    isInit = true;
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
        	} else if (intent.getAction().equals(CMD_NOTIFICATION_ADDED)) {
        		Botification not = intent.getParcelableExtra("notification");
        		not.load(mService);
        		notificationAdded(not);        		
        	} else if (intent.getAction().equals(CMD_NOTIFICATION_REMOVED)) {
        		Botification not_todelete = intent.getParcelableExtra("notification");
        		for (int i = mNotifications.size() -1; i >=0 ; i--) {
        			Botification not = mNotifications.get(i);
    				if (not.equals(not_todelete)) {
    					removeNotification();
    				} else {
    					removeNotification(not);
    				}
    				return;
        				
    			}
        	}
        }
    };
    
    private boolean isActive() {
    	return mAudioManager.isBluetoothA2dpOn() || !mSharedPref.getBoolean("metadata_bt_only", true);
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
    	
    	((NotificationInterface)mService).cancelNotification(old);
    	mNotifications.remove(old);
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
		if (mSharedPref.getBoolean("action_tts", false) && !notify.mRead &&
				(mAudioManager.isBluetoothA2dpOn() || !mSharedPref.getBoolean("tts_bt_only", true))) {
			String txt = notify.getPreference("tts_value", true);
        	mTTS.speak(txt, TextToSpeech.QUEUE_FLUSH, null);
        	notify.mRead = true;
        }
		if (isActive()) {
			Log.d(TAG, "Setting Metadata");
	        showNotify(notify.getPreference("metadata_artist"), notify.getPreference("metadata_album"), notify.getPreference("metadata_title"), notify.mNotification.number);
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
	}
	
    private void setUpRemoteControlClient() {
        mMediaButtonReceiverComponent = new ComponentName(mService.getPackageName(), MediaButtonIntentReceiver.class.getName());
    	mAudioManager.registerMediaButtonEventReceiver(mMediaButtonReceiverComponent);
        final Intent mediaButtonIntent = new Intent(Intent.ACTION_MEDIA_BUTTON);
        mediaButtonIntent.setComponent(mMediaButtonReceiverComponent);
        mRemoteControlClient = new RemoteControlClient(
                PendingIntent.getBroadcast(mService.getApplicationContext(), 0, mediaButtonIntent,
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
    
        
    private boolean isBlackListed(String txt) {
    	Set<String> blacklist = mSharedPref.getStringSet("blacklistentries", null);
    	if (blacklist != null) {
    		for (String entry : blacklist) {
    			entry = entry.replace(".", "\\.").replace("*", ".*");
    			Pattern pat = Pattern.compile(entry, Pattern.DOTALL);
				if (pat.matcher(txt).matches()) {
					Log.d(TAG, txt + " matches " + entry);
					return true;
				}
			}
    	}
    	return false;
    }
    
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
		Log.d(TAG, "Service interrupted");
		mAudiofocus = -1;
		mAudioManager.abandonAudioFocus(mAudioFocusListener);
	    isInit = false;
	}
	
	public void notificationAdded(Botification notification) {
		if (isBlackListed(notification.mText)) {
			return;
		}
        addNotification(notification);
        showNotify(notification);
	}

	@Override
	public void onInit(int status) {
		// is part of TTS listener
		
	}
	
}