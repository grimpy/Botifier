package com.github.grimpy.botifier;


import android.annotation.TargetApi;
import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

@TargetApi(18)
public class BotifierNotificationService extends NotificationListenerService implements NotificationInterface{
	private static String TAG = "Botifier";
	private BotifierManager mBotifyManager;
	private Handler mHandler;

	
	@Override
	public void onCreate() {
		super.onCreate();
		Log.i(TAG, "Manager started");
		mBotifyManager = new BotifierManager(this);
	    mHandler = new Handler(){
	    	public void handleMessage(Message msg){
	    		String cmd = BotifierManager.CMD_NOTIFICATION_ADDED;
	    		if (msg.arg1 == 1) {
	    			cmd = BotifierManager.CMD_NOTIFICATION_REMOVED;
	    		}
	    		StatusBarNotification stn = (StatusBarNotification)msg.obj;
	    		if (stn == null || !mBotifyManager.isIntresting(stn.getNotification())) {
	    			return;
	    		}
	    		Notification not = stn.getNotification();
	    		if (not == null) {
	    			return;
	    		}
	    		String description = "";
	    		if (not.tickerText != null) {
	    			description = not.tickerText.toString();
	    		}
	    		Service srv = BotifierNotificationService.this;
	    		String text = Botification.extractTextFromNotification(srv, not);
	    		Botification bot = new Botification(stn.getId(), stn.getPackageName(), stn.getTag(), description, text);
	    		Intent i = new Intent(cmd);
	    		i.putExtra("botification", bot);
	    		sendBroadcast(i);
	    		//Looper.myLooper().quit();
		    }       
		};
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		mBotifyManager.destroy();
	}
	
	@Override
	public void onNotificationPosted(StatusBarNotification statusnotification) {
		Log.i(TAG, "new notification received");
		Message msg = new Message();
		msg.obj = statusnotification;
		msg.arg1 = 0;
		mHandler.sendMessage(msg);
	}

	@Override
	public void onNotificationRemoved(StatusBarNotification statusnotification) {
		Log.d(TAG, "Cleaning up notifications");
		Message msg = new Message();
		msg.obj = statusnotification;
		msg.arg1 = 1;
		mHandler.sendMessage(msg);
	}
	
	public void cancelNotification(Botification not){
		cancelNotification(not.mPkg, not.mTag, not.mId);
	}
	
	
}