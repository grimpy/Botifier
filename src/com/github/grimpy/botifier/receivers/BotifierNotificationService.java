package com.github.grimpy.botifier.receivers;


import android.annotation.TargetApi;
import android.app.Notification;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Message;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import com.github.grimpy.botifier.Botification;
import com.github.grimpy.botifier.NotificationEvents;

@TargetApi(18)
public class BotifierNotificationService extends NotificationListenerService{
	private static String TAG = "Botifier";
    public static String REMOVE_NOTIFICATION = "com.github.grimpy.botifier.REMOVE_NOTIFICATION";
	private Handler mHandler;


    private final BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        /**
         * {@inheritDoc}
         */
        @Override
        public void onReceive(final Context context, final Intent intent) {
            Log.d(TAG, "Received action " + intent.getAction());
            if (intent.getAction().equals(NOTIFICATION_SERVICE) ) {
                Botification bot = intent.getParcelableExtra("botification");
                cancelNotification(bot);
            }
        }
    };

	
	@Override
	public void onCreate() {
		super.onCreate();
		Log.i(TAG, "Manager started");
        final IntentFilter filter = new IntentFilter();
        filter.addAction(REMOVE_NOTIFICATION);
        // Attach the broadcast listener
        registerReceiver(mIntentReceiver, filter);
	    mHandler = new Handler(){
	    	public void handleMessage(Message msg){
	    		String cmd = NotificationEvents.NOTIFICATION_ADDED;
	    		if (msg.arg1 == 1) {
	    			cmd = NotificationEvents.NOTIFICATION_REMOVED;
	    		}
	    		StatusBarNotification stn = (StatusBarNotification)msg.obj;
	    		if (stn == null) {
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
	    		android.app.Service srv = BotifierNotificationService.this;
	    		String text = Botification.extractTextFromNotification(srv, not);
	    		Botification bot = new Botification(stn.getId(), stn.getPackageName(), stn.getTag(), description, text);
	    		bot.load(BotifierNotificationService.this);
	    		if (bot.isBlackListed() || !bot.isIntresting(not)) {
	    		    return;
	    		}
	    		Intent i = new Intent(cmd);
	    		i.putExtra("botification", bot);
	    		sendBroadcast(i);
		    }
		};
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
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