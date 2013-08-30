package com.github.grimpy.botifier;

import java.util.ArrayList;
import java.util.Set;
import java.util.regex.Pattern;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.media.RemoteControlClient;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.media.RemoteControlClient.MetadataEditor;
import android.preference.PreferenceManager;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RemoteViews;
import android.widget.TextView;
import android.view.KeyEvent;


public class BotifierService extends NotificationListenerService implements OnInitListener {

	public static final String SERVICECMD = "com.github.grimpy.botifier.cmd";
	public static final String NOTIFICATION = "com.github.grimpy.botifier.notification";
	public static final String CMD_NOTIFICATION_ADDED = "com.github.grimpy.botifier.notification.added";
	public static final String CMD_NOTIFICATION_REMOVED = "com.github.grimpy.botifier.notification.removed"; 
	
	private static final int TIMESTAMPID = 16908388;
	private boolean isInit = false;
	private static String TAG = "Botifier";
	private SharedPreferences mSharedPref;

	private RemoteControlClient mRemoteControlClient;
	private AudioManager mAudioManager;
	private ComponentName mMediaButtonReceiverComponent;
	private PackageManager mPackageManager;
	private ArrayList<Notifies> mNotifications;
	private int mCurrent = -1;
	private LayoutInflater mInflater;
	private int mAudiofocus = -1;
	private TextToSpeech mTTS;



	class Notifies {
		public String mPackageName;
		public String mPackageLabel;
		public String mDescription;
		public String mText;
		public StatusBarNotification mNotification;
		public int mOffset;
		public Notifies(String packageName, String packageLabel, ArrayList<String> text, StatusBarNotification notification) {
			mPackageLabel = packageLabel;
			mPackageName = packageName;
			mDescription = notification.getNotification().tickerText.toString();
			mText = TextUtils.join("\n", text);

			mNotification = notification;
			mOffset = 0;
		}
		
		public boolean hasNext() {
			int maxlength = Integer.valueOf(mSharedPref.getString("maxlength", "0"));
			if (maxlength == 0) {
				return false;
			}
			return (mOffset+1)*maxlength < mText.length();
		}
		
		public String getPreference(String key) {
			String message = mSharedPref.getString(key, "");
			int maxlength = Integer.valueOf(mSharedPref.getString("maxlength", "0"));
			message = message.replace("%f", toString());
			message = message.replace("%a", mPackageLabel);
			message = message.replace("%d", mDescription);
			message = message.replace("%m", mText);
			
			if (maxlength != 0 && message.length() > maxlength) {
				int start = mOffset * maxlength;
				int end = start + maxlength;
				if (end >= message.length()) {
					end = message.length() -1;
					mOffset = -1;
				}
				String result = message.substring(start, end);
				mOffset++;
				return result;
			}
			return message;
		}
		
		public String toString() {
			return String.format("%s %s %s", mPackageLabel, mDescription, mText);
		}

		@Override
		public boolean equals(Object o) {
			if (Notifies.class.isInstance(o)) {
				Notifies not = (Notifies) o;
				if (not.mNotification.getId() == mNotification.getId() &&
				    not.mNotification.getPackageName() == mNotification.getPackageName() &&
				    not.mNotification.getUserId() == mNotification.getUserId()) {
					return true;
				}
			}
			return false;
		}
		

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
        	} else if (intent.getAction().equals(NOTIFICATION)) {
        		String album = intent.getStringExtra("album");
        		String artist = intent.getStringExtra("artist");
        		String title = intent.getStringExtra("title");

        		showNotify(album, artist, title, 10);
        	} else if (intent.getAction().equals(CMD_NOTIFICATION_ADDED)) {
        		notificationAdded((StatusBarNotification) intent.getParcelableExtra("notification"));
        	}
        }
    };
    
    public boolean isActive() {
    	return mAudioManager.isBluetoothA2dpOn() || true;
    }
    
    private void removeNotification() {
    	if (mCurrent == -1 || mCurrent > mNotifications.size() -1) {
    		resetNotify();
    		return;
    	}
    	Log.d(TAG, "Remove current notification: " + mCurrent);
    	Notifies old = mNotifications.get(mCurrent);
    	removeNotification(old);
    } 	
    private void removeNotification(Notifies old) {
    	cancelNotification(old.mNotification.getPackageName(), old.mNotification.getTag(), old.mNotification.getId());
    	mNotifications.remove(old);
    	if (mNotifications.size() == 0) {
    		mCurrent = -1;
    		resetNotify();
    		return;
    	}
    	showNotify(0, true);

    }
    
    private void getAudioFocus() {
    	if (true || mAudiofocus != AudioManager.AUDIOFOCUS_GAIN) {
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
    	Log.d(TAG, "Move notification with offset " + offset + " currnetidx: " + idx);
    	
    	if (idx >= mNotifications.size()) {
    		idx = 0;
    	} else if (idx < 0) {
    		idx = mNotifications.size() -1;
    	}
    	if (mCurrent >= mNotifications.size()){
    		mCurrent = mNotifications.size() -1;
    	}
    	Log.d(TAG, "Move new idx " + idx + " size: " + mNotifications.size());
    	Notifies current = mNotifications.get(mCurrent);
    	if (next || ( offset > 0 && mCurrent != -1 && !current.hasNext())) {
    		showNotify(mNotifications.get(idx));
    	} else {
    		showNotify(current);
    	}
    	
    }
    
    private void resetNotify() {
        showNotify("Botifier", "Botifier", "Botifier", 0);
    	//mAudioManager.abandonAudioFocus(mAudioFocusListener);
        //mAudioManager.unregisterMediaButtonEventReceiver(mMediaButtonReceiverComponent);
    }
	
	public void showNotify(Notifies notify) {
		Log.d(TAG, "Setting notification " + notify.toString());
		mCurrent = mNotifications.indexOf(notify);
		if (mSharedPref.getBoolean("action_tts", false) && notify.mOffset == 0) {
        	mTTS.speak(notify.mText, TextToSpeech.QUEUE_ADD, null);
        }
		if (isActive()) {
			Log.d(TAG, "Setting Metadata");
	        showNotify(notify.getPreference("metadata_artist"), notify.getPreference("metadata_album"), notify.getPreference("metadata_title"), notify.mNotification.getNotification().number);
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
                AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);

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
    
    private void extractViewType(ArrayList<View> outViews, Class viewtype, View source) {
    	if (ViewGroup.class.isInstance(source)) {
    		ViewGroup vg = (ViewGroup) source;
    		for (int i = 0; i < vg.getChildCount(); i++) {
    			extractViewType(outViews, viewtype, vg.getChildAt(i));
				
			}
    	} else if(viewtype.isInstance(source)) {
			outViews.add(source);
    	}
    }
    
    private ArrayList<String> extractTextFromNotification(Notification notification) {
    	ArrayList<String> result = null;
	    result =  extractTextFromNotification(notification.bigContentView);
	    if (result == null) {
	    	result = extractTextFromNotification(notification.contentView);
	    }
	    return result;

    }

    
    private ArrayList<String> extractTextFromNotification(RemoteViews view) {
	    ArrayList<String> result = new ArrayList<String>();
	    if (view == null) {
	    	Log.d(TAG, "View is empty");
	    	return null;
	    }
		try {
			int layoutid = view.getLayoutId();
			ViewGroup localView = (ViewGroup) mInflater.inflate(layoutid, null);
		    view.reapply(getApplicationContext(), localView);
		    ArrayList<View> outViews = new ArrayList<View>();
		    extractViewType(outViews, TextView.class, localView);
		    for (View  ttv: outViews) {
		    	TextView tv = (TextView) ttv;
		    	String txt = tv.getText().toString();
		    	if (!TextUtils.isEmpty(txt) && tv.getId() != TIMESTAMPID) {
		    		result.add(txt);
		    	}
			}
		} catch (Exception e) {
			Log.d(TAG, "FAILED to load notification " + e.toString());
			Log.wtf(TAG, e);
			return null;
			//notification might have dissapeared by now
		}
		Log.d(TAG, "Return result" + result);
	    return result;
    }
        
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
    
	private void addNotification(Notifies notification) {
		for (int i = 0; i < mNotifications.size(); i++) {
			Notifies not = mNotifications.get(i);
			Log.d(TAG, "Adding notification comparing with " + not.mPackageName);
			if (not.equals(notification)) {
				mNotifications.set(i, notification);
				return;
			}
		}
		mNotifications.add(notification);
	}
	
	private String getPackageLabel(String packagename){
		ApplicationInfo ai;
		try {
		    ai = mPackageManager.getApplicationInfo( packagename, 0);
		} catch (final NameNotFoundException e) {
		    ai = null;
		}
		return (String) (ai != null ? mPackageManager.getApplicationLabel(ai) : packagename);

	}

   @Override
    public void onCreate() {
	    super.onCreate();
	    if (isInit) {
	        return;
	    }
	    
        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        mMediaButtonReceiverComponent = new ComponentName(getPackageName(), MediaButtonIntentReceiver.class.getName());
    	mAudioManager.registerMediaButtonEventReceiver(mMediaButtonReceiverComponent);
		// build the PendingIntent for the remote control client
		//setUpRemoteControlClient();
		
		mPackageManager = getApplicationContext().getPackageManager();
		mNotifications = new ArrayList<BotifierService.Notifies>();
		mTTS = new TextToSpeech(this, this);
		
        final IntentFilter filter = new IntentFilter();
        filter.addAction(SERVICECMD);
        filter.addAction(NOTIFICATION);
        filter.addAction(CMD_NOTIFICATION_ADDED);
        filter.addAction(CMD_NOTIFICATION_REMOVED);
        // Attach the broadcast listener
        registerReceiver(mIntentReceiver, filter);
        mInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mSharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        
	    isInit = true;
	}

	public void onDestroy() {
		super.onDestroy();
		Log.d(TAG, "Service interrupted");
		mAudiofocus = -1;
		mAudioManager.abandonAudioFocus(mAudioFocusListener);
	    isInit = false;
	}
	
	public void notificationAdded(StatusBarNotification statusnotification) {
		Log.i(TAG, "Received notification " + statusnotification.toString());
		String packageName = statusnotification.getPackageName().toString();
		Notification notification = statusnotification.getNotification();
		if (notification == null) {
			return;
		}
		ArrayList<String> txt = extractTextFromNotification(notification);
		if (txt == null || isBlackListed(TextUtils.join("\n", txt))) {
			return;
		}

		String appname = getPackageLabel(packageName);

        Notifies not = new Notifies(packageName, appname, txt, statusnotification);
        addNotification(not);
        showNotify(not);
	}

	@Override
	public void onNotificationPosted(StatusBarNotification statusnotification) {
		if (statusnotification.isOngoing()) {
			return;
		}
		Intent i = new Intent(CMD_NOTIFICATION_ADDED);
		i.putExtra("notification", statusnotification);
		sendBroadcast(i);
		
	}

	@Override
	public void onNotificationRemoved(StatusBarNotification statusnotification) {
		Log.d(TAG, "Cleaning up notifications");
		for (int i = mNotifications.size() -1; i >=0 ; i--) {
			Notifies not = mNotifications.get(i);
			if (not.mNotification.getId() == statusnotification.getId()) {
				if (not.equals(mCurrent)) {
					removeNotification();
				} else {
					removeNotification(not);
				}
				return;
				
			}
		}

		
	}
	@Override
	public void onInit(int status) {
		// is part of TTS listener
		
	}
	
}