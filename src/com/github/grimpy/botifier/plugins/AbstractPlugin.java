package com.github.grimpy.botifier.plugins;
import android.app.Service;
import android.content.Intent;

import com.github.grimpy.botifier.Botification;
import com.github.grimpy.botifier.NotificationEvents;
import com.github.grimpy.botifier.NotificationInterface;
import com.github.grimpy.botifier.receivers.BotifierNotificationService;

public abstract class AbstractPlugin extends Service implements NotificationInterface {

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent.getAction().equals(NotificationEvents.NOTIFICATION_ADDED)) {
            Botification bot = intent.getParcelableExtra("botification");
            bot.load(this);
            notificationAdded(bot);
        } else if (intent.getAction().equals(NotificationEvents.NOTIFICATION_REMOVED)) {
            Botification bot = intent.getParcelableExtra("botification");
            notificationRemoved(bot);
        }
        return START_STICKY;
    }

    protected String _(int id){
        return getString(id);
    }

    protected void removeNotifcation(Botification bot){
        Intent intent = new Intent();
        intent.setAction(BotifierNotificationService.REMOVE_NOTIFICATION);
        intent.putExtra("botification", bot);
        sendBroadcast(intent);
    }
}