package com.github.grimpy.botifier;


import android.annotation.TargetApi;
import android.content.Intent;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

@TargetApi(18)
public class BotifierNotificationService extends NotificationListenerService implements NotificationInterface{
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
	
	@Override
	public void onNotificationPosted(StatusBarNotification statusnotification) {
		Log.i(TAG, "new notification received");
		if (statusnotification.isOngoing()) {
			return;
		}
		sendCmd(statusnotification, BotifierManager.CMD_NOTIFICATION_ADDED);
	}

	private void sendCmd(StatusBarNotification stn, String cmd) {
		Intent i = new Intent(cmd);
		Botification not = new Botification(stn.getNotification(), stn.getId(), stn.getPackageName(), stn.getTag());
		i.putExtra("notification", not);
		sendBroadcast(i);
		
	}

	@Override
	public void onNotificationRemoved(StatusBarNotification statusnotification) {
		Log.d(TAG, "Cleaning up notifications");
		sendCmd(statusnotification, BotifierManager.CMD_NOTIFICATION_REMOVED);
	}
	
	public void cancelNotification(Botification not){
		cancelNotification(not.mPkg, not.mTag, not.mId);
	}
	
	
}