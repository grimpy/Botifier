package com.github.grimpy.botifier;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.Notification;
import android.content.Intent;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;

public class BotifierAccessibilityService extends AccessibilityService implements NotificationInterface {

	@Override
	protected void onServiceConnected() {
		super.onServiceConnected();
		AccessibilityServiceInfo info = new AccessibilityServiceInfo();
		info.eventTypes = AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED | AccessibilityEvent.TYPE_ANNOUNCEMENT;
		info.feedbackType = AccessibilityServiceInfo.FEEDBACK_ALL_MASK;
		setServiceInfo(info); 
	}

	private static String TAG = "Botifier";
	private BotifierManager mBotifyManager; 

	
	@Override
	public void onCreate() {
		super.onCreate();
		Log.i(TAG, "Manager started");
		mBotifyManager = new BotifierManager(this);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		mBotifyManager.destroy();
	}
	
	
	private void sendCmd(Botification not, String cmd) {
		Intent i = new Intent(cmd);
		i.putExtra("notification", not);
		sendBroadcast(i);
		
	}

	
	public void onAccessibilityEvent(AccessibilityEvent event) {
		if (event.getEventType() == AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED) {
			Notification notification = (Notification) event.getParcelableData();
	        if (notification == null) {
	        	return;
	        }
	        Botification not = new Botification(notification, 0, event.getPackageName().toString(), "");
	        sendCmd(not, BotifierManager.CMD_NOTIFICATION_ADDED);
	      }
    } 

	@Override
	public void onInterrupt() {
		// TODO Auto-generated method stub	
	}

	@Override
	public void cancelNotification(Botification not) {
		// TODO Auto-generated method stub
		
	}
}