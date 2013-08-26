package com.github.grimpy.botifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.Notification;
import android.app.NotificationManager;
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
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.widget.ImageView;
import android.widget.RemoteViews;
import android.widget.TextView;
import android.view.KeyEvent;


public class BotifierService extends AccessibilityService implements OnInitListener {

	public static final String SERVICECMD = "com.github.grimpy.botifier.cmd";
	public static final String NOTIFICATION = "com.github.grimpy.botifier.notification";
	private static final int TIMESTAMPID = 16908388;
	private boolean isInit = false;
	private static String TAG = "Botifier";
	private SharedPreferences mSharedPref;

	private RemoteControlClient mRemoteControlClient;
	private AudioManager mAudioManager;
	private ComponentName mMediaButtonReceiverComponent;
	private PackageManager mPackageManager;
	private ArrayList<Notifies> mNotifications;
	private Notifies mCurrent;
	private NotificationManager mNM;
	private LayoutInflater mInflater;
	private int mAudiofocus = -1;
	private TextToSpeech mTTS;



	class Notifies {
		public String mPackageName;
		public String mPackageLabel;
		public ArrayList<String> mDescription;
		public String mText;
		public Notification mNotification;
		public int mOffset;
		public Notifies(String packageName, String packageLabel, ArrayList<String> text, List<CharSequence> description, Notification notification) {
			mPackageLabel = packageLabel;
			mPackageName = packageName;
			mDescription = new ArrayList<String>();
			mText = TextUtils.join("\n", text);

			for (int i = 0; i < description.size(); i++) {
				CharSequence chr = description.get(i);
				if (chr != null) {
					mDescription.add(chr.toString());
				}
			}
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
			String type = mSharedPref.getString(key, "");
			int maxlength = Integer.valueOf(mSharedPref.getString("maxlength", "0"));
			if (type.equals("all")) {
				return toString();
			} else if (type.equals("appname")) {
				return mPackageLabel;
			} else if (type.equals("message")) {
				if (maxlength != 0 && mText.length() > maxlength) {
					int start = mOffset * maxlength;
					int end = start + maxlength;
					if (end >= mText.length()) {
						end = mText.length() -1;
						mOffset = -1;
					}
					String result = mText.substring(start, end);
					mOffset++;
					return result;
				}
				return mText;
			} else if (type.equals("description")) {
				return TextUtils.join("\n", mDescription);
			}
			return "CUSTOM";
		}
		
		public String toString() {
			return String.format("%s %s %s", mPackageLabel, mDescription, mText);
		}
	}
	
    private final BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        /**
         * {@inheritDoc}
         */
        @Override
        public void onReceive(final Context context, final Intent intent) {
        	if (intent.getAction() == SERVICECMD ) {
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
        	} else {
        		String album = intent.getStringExtra("album");
        		String artist = intent.getStringExtra("artist");
        		String title = intent.getStringExtra("title");

        		showNotify(album, artist, title, 10);
        	}
        }
    };
    
    private void removeNotification() {
    	if (mCurrent == null) {
    		resetNotify();
    		return;
    	}
    	Log.d(TAG, "Remove current notification: " + mCurrent);
    	Notifies old = mCurrent;
    	showNotify(1, true);
    	mNotifications.remove(old);
    	if (mNotifications.size() == 0) {
    		mCurrent = null;
    		resetNotify();
    		return;
    	}

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
    		mCurrent = null;
    		return;
    	}
    	int idx = mNotifications.indexOf(mCurrent) + offset;
    	Log.d(TAG, "Move notification with offset " + offset + " currnetidx: " + idx);
    	
    	if (idx >= mNotifications.size()) {
    		idx = 0;
    	} else if (idx < 0) {
    		idx = mNotifications.size() -1;
    	}
    	Log.d(TAG, "Move new idx " + idx + " size: " + mNotifications.size());
    	if (next || ( offset > 0 && mCurrent != null && !mCurrent.hasNext())) {
    		showNotify(mNotifications.get(idx));
    	} else {
    		showNotify(mCurrent);
    	}
    	
    }
    
    private void resetNotify() {
        showNotify("Botifier", "Botifier", "Botifier", 0);
    	mAudioManager.abandonAudioFocus(mAudioFocusListener);
        mAudioManager.unregisterMediaButtonEventReceiver(mMediaButtonReceiverComponent);
    }
	
	public void showNotify(Notifies notify) {
		Log.d(TAG, "Setting notification " + notify.toString());
		mCurrent = notify;
		if (mAudioManager.isBluetoothA2dpOn()) {
			Log.d(TAG, "Setting Metadata");
	        showNotify(notify.getPreference("metadata_artist"), notify.getPreference("metadata_album"), notify.getPreference("metadata_title"), notify.mNotification.number);
		}
	}
	
	public void showNotify(String artist, String album, String title, int tracknr) {
        getAudioFocus();
        if (mSharedPref.getBoolean("action_tts", false)) {
        	mTTS.speak(title, TextToSpeech.QUEUE_ADD, null);
        }
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
	    
		try {
			ViewGroup localView = (ViewGroup) mInflater.inflate(view.getLayoutId(), null);
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
			Log.d(TAG, "FAILED to load notification");
			return null;
			//notification might have dissapeared by now
		}
		Log.d(TAG, "Return result" + result);
	    return result;
    }
    
    private boolean isNotificationAlive(Notification notification) {
    	RemoteViews view = notification.contentView;
    	ViewGroup localView = (ViewGroup) mInflater.inflate(view.getLayoutId(), null);
	    view.reapply(getApplicationContext(), localView);
	    ArrayList<View> outViews = new ArrayList<View>();
	    extractViewType(outViews, ImageView.class, localView);
	    for (View iview : outViews) {
			ImageView imageview = (ImageView) iview;
			if (imageview.getDrawable() == null) {
				return false;
			}
		}
    	return true;
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
    
	@Override
	public void onAccessibilityEvent(AccessibilityEvent event) {
		String packageName = event.getPackageName().toString();
		if (packageName.equals("com.github.grimpy.btnotifier") || !mAudioManager.isBluetoothA2dpOn() ) {
			return;
		}
		
		if (event.getEventType() == AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED) {
			Notification notification = (Notification) event.getParcelableData();
			if (notification == null) {
				return;
			}
			ArrayList<String> txt = extractTextFromNotification(notification);
			if (txt == null || isBlackListed(TextUtils.join("\n", txt))) {
				return;
			}

			String appname = getPackageLabel(packageName);
			Log.d(TAG, String.format("Received event package=%s text: %s notdesc=%s action=%d type=%s data=%s",
					appname,
					event.getText(), 
					txt.toString(),
					event.getAction(),
					AccessibilityEvent.eventTypeToString(event.getEventType()),
					notification));

	        Notifies not = new Notifies(packageName, appname, txt, event.getText(), notification);
	        addNotification(not);
	        showNotify(not);
		    
	    }
		else {
			cleanupNotifications();
		}
	}
	
	private void cleanupNotifications() {
		Log.d(TAG, "Cleaning up notifications");
		for (int i = mNotifications.size() -1; i >=0 ; i--) {
			Notifies not = mNotifications.get(i);
			boolean alive = isNotificationAlive(not.mNotification);
			Log.d(TAG, "Checking " + not + " alive " + alive);
			Log.d(TAG, "Removing notification" + not.toString());
			//mNotifications.remove(i);
																																	}
	}
	
	private void addNotification(Notifies notification) {
		for (int i = 0; i < mNotifications.size(); i++) {
			Notifies not = mNotifications.get(i);
			Log.d(TAG, "Adding notification comparing with " + not.mPackageName);
			if (not.mPackageName.equals(notification.mPackageName)) {
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
	protected void onServiceConnected() {
	    if (isInit) {
	        return;
	    }
	    AccessibilityServiceInfo info = new AccessibilityServiceInfo();
	    info.eventTypes = AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED | AccessibilityEvent.TYPE_ANNOUNCEMENT;
	    info.feedbackType = AccessibilityServiceInfo.FEEDBACK_ALL_MASK;
	    setServiceInfo(info);
	    
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
        // Attach the broadcast listener
        registerReceiver(mIntentReceiver, filter);
        mInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mSharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        
	    isInit = true;
	}

	@Override
	public void onInterrupt() {
		Log.d(TAG, "Service interrupted");
		mAudiofocus = -1;
		mAudioManager.abandonAudioFocus(mAudioFocusListener);
	    isInit = false;
	}

	@Override
	public void onInit(int status) {
		// TODO Auto-generated method stub
		
	}
	
}