package com.github.grimpy.botifier.receivers;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.Notification;
import android.content.Intent;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;

import com.github.grimpy.botifier.Botification;
import com.github.grimpy.botifier.NotificationEvents;

public class BotifierAccessibilityService extends AccessibilityService {

	@Override
	protected void onServiceConnected() {
		super.onServiceConnected();
		AccessibilityServiceInfo info = new AccessibilityServiceInfo();
		info.eventTypes = AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED | AccessibilityEvent.TYPE_ANNOUNCEMENT;
		info.feedbackType = AccessibilityServiceInfo.FEEDBACK_ALL_MASK;
		setServiceInfo(info); 
	}

	private static String TAG = "Botifier";

	
	@Override
	public void onCreate() {
		super.onCreate();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
	}
	
	
	private void sendCmd(AccessibilityEvent event, Notification not, String cmd) {
		Intent i = new Intent(cmd);
		String description = "";
		if (not.tickerText != null) {
			not.tickerText.toString();
		}
		String text = Botification.extractTextFromNotification(this, not);
		Botification bot = new Botification(0, event.getPackageName().toString(), "", description, text);
		bot.load(this);
        if (bot.isBlackListed() || !bot.isIntresting(not)) {
		    return;
		}
		i.putExtra("botification", bot);
		sendBroadcast(i);		
	}

	
	public void onAccessibilityEvent(AccessibilityEvent event) {
		if (event.getEventType() == AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED) {
			Notification notification = (Notification) event.getParcelableData();
			if (notification == null ) {
	        	return;
	        }
			sendCmd(event, notification, NotificationEvents.NOTIFICATION_ADDED);
	      }
    } 

	@Override
	public void onInterrupt() {
		// TODO Auto-generated method stub	
	}

}