package com.github.grimpy.botifier.plugins.liveware;

import com.github.grimpy.botifier.Botification;
import com.github.grimpy.botifier.NotificationEvents;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.database.SQLException;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.ShareActionProvider;
import android.widget.Toast;

import com.github.grimpy.botifier.R;
import com.github.grimpy.botifier.receivers.BotifierNotificationService;
import com.sonyericsson.extras.liveware.aef.notification.Notification;
import com.sonyericsson.extras.liveware.aef.registration.Registration;
import com.sonyericsson.extras.liveware.extension.util.ExtensionService;
import com.sonyericsson.extras.liveware.extension.util.notification.NotificationUtil;
import com.sonyericsson.extras.liveware.extension.util.registration.DeviceInfoHelper;
import com.sonyericsson.extras.liveware.extension.util.registration.RegistrationInformation;

/**
 * The sample extension service handles extension registration and inserts data
 * into the notification database.
 */
public class SWExtensionService extends ExtensionService {
    /**
     * Extensions specific id for the source
     */
    public static final String EXTENSION_SPECIFIC_ID = "EXTENSION_SPECIFIC_ID_SAMPLE_NOTIFICATION";

    /**
     * Extension key
     */
    public static final String EXTENSION_KEY = "com.github.grimpy.botifier.plugins.liveware.key";

    /**
     * Log tag
     */
    public static final String LOG_TAG = "Botifier";

    /**
     * Event names
     */
    public static final String INTENT_ACTION_CLEAR = "com.sonymobile.smartconnect.extension.notificationsample.action.stop";

    public SWExtensionService() {
        super(EXTENSION_KEY);
    }

    /**
     * {@inheritDoc}
     *
     * @see android.app.Service#onStartCommand()
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        int result = super.onStartCommand(intent, flags, startId);
        if (intent != null) {
            Log.d(LOG_TAG, "onStart action: " + intent.getAction());
            if (INTENT_ACTION_CLEAR.equals(intent.getAction())) {
                Log.d(LOG_TAG, "onStart action: INTENT_ACTION_STOP");
                clearData();
                stopSelfCheck();
            } else if (intent.getAction().equals(NotificationEvents.NOTIFICATION_ADDED)) {
                Botification bot = intent.getParcelableExtra("botification");
                bot.load(SWExtensionService.this);
                addData(bot);
                return 0;
            } else if (intent.getAction().equals(NotificationEvents.NOTIFICATION_REMOVED)) {
                Botification bot = intent.getParcelableExtra("botification");
                removeData(bot);
                return 0;
            }

        }
        return result;
    }
    
    /**
     * {@inheritDoc}
     *
     * @see android.app.Service#onDestroy()
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(LOG_TAG, "onDestroy");
    }

    /**
     * Cancel scheduled data insertion
     */
    private void clearData() {
        NotificationUtil.deleteAllEvents(this);
    }

    private void removeData(Botification bot) {

        SharedPreferences mSharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        String where = Notification.EventColumns.FRIEND_KEY + "='" + bot.getID() + "'";
        if (mSharedPref.getBoolean(getString(R.string.pref_sw_keep), false)) {
            ContentValues cv = new ContentValues();
            cv.put(Notification.EventColumns.EVENT_READ_STATUS, true);
            getContentResolver().update(Notification.Event.URI, cv, where, null);
        } else {
            getContentResolver().delete(Notification.Event.URI, where ,null);
        }
    }

    protected void removeNotifcation(Botification bot){
        Intent intent = new Intent();
        intent.setAction(BotifierNotificationService.REMOVE_NOTIFICATION);
        intent.putExtra("botification", bot);
        sendBroadcast(intent);
    }

    private void addData(Botification bot) {
        //remove old data from same app/data
        removeData(bot);
        String name = bot.mPackageLabel;
        String message = bot.mText;
        PackageManager pm = this.getPackageManager();
        int iconresid = 0;
        try {
            iconresid = pm.getApplicationInfo(bot.mPkg, PackageManager.GET_META_DATA).icon;
        } catch (NameNotFoundException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        String image = new Uri.Builder().scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
                .authority(bot.mPkg).appendPath(Integer.toString(iconresid))
                .toString();
        
        long time = System.currentTimeMillis();
        long sourceId = NotificationUtil
                .getSourceId(this, EXTENSION_SPECIFIC_ID);
        if (sourceId == NotificationUtil.INVALID_ID) {
            Log.e(LOG_TAG, "Failed to insert data");
            return;
        }
        ContentValues eventValues = new ContentValues();
        eventValues.put(Notification.EventColumns.EVENT_READ_STATUS, false);
        eventValues.put(Notification.EventColumns.DISPLAY_NAME, name);
        eventValues.put(Notification.EventColumns.MESSAGE, message);
        eventValues.put(Notification.EventColumns.PERSONAL, 1);
        eventValues.put(Notification.EventColumns.FRIEND_KEY, bot.getID());
        eventValues.put(Notification.EventColumns.PROFILE_IMAGE_URI, image);
        eventValues.put(Notification.EventColumns.PUBLISHED_TIME, time);
        eventValues.put(Notification.EventColumns.SOURCE_ID, sourceId);

        try {
            getContentResolver().insert(Notification.Event.URI, eventValues);
        } catch (IllegalArgumentException e) {
            Log.e(LOG_TAG, "Failed to insert event", e);
        } catch (SecurityException e) {
            Log.e(LOG_TAG, "Failed to insert event, is Live Ware Manager installed?", e);
        } catch (SQLException e) {
            Log.e(LOG_TAG, "Failed to insert event", e);
        }
    }

    @Override
    protected void onViewEvent(Intent intent) {
        String action = intent.getStringExtra(Notification.Intents.EXTRA_ACTION);
        String hostAppPackageName = intent
                .getStringExtra(Registration.Intents.EXTRA_AHA_PACKAGE_NAME);
        boolean advancedFeaturesSupported = DeviceInfoHelper.isSmartWatch2ApiAndScreenDetected(
                this, hostAppPackageName);

        int eventId = intent.getIntExtra(Notification.Intents.EXTRA_EVENT_ID, -1);
        if (Notification.SourceColumns.ACTION_1.equals(action)) {
            NotificationUtil.deleteAllEvents(this);
        } else if (Notification.SourceColumns.ACTION_2.equals(action)) {
            NotificationUtil.deleteAllEvents(this);
        } else if (Notification.SourceColumns.ACTION_3.equals(action)) {
            Toast.makeText(this, "Action 3", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onRefreshRequest() {
        // Do nothing here, only relevant for polling extensions, this
        // extension is always up to date
    }

    /**
     * Show toast with event information
     *
     * @param eventId The event id
     */
    public void doAction1(int eventId) {
        Log.d(LOG_TAG, "doAction1 event id: " + eventId);
        Cursor cursor = null;
        try {
            String name = "";
            String message = "";
            cursor = getContentResolver().query(Notification.Event.URI, null,
                    Notification.EventColumns._ID + " = " + eventId, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int nameIndex = cursor.getColumnIndex(Notification.EventColumns.DISPLAY_NAME);
                int messageIndex = cursor.getColumnIndex(Notification.EventColumns.MESSAGE);
                name = cursor.getString(nameIndex);
                message = cursor.getString(messageIndex);
            }

            String toastMessage = "Event: " + eventId
                    + ", Name: " + name + ", Message: " + message;
            Toast.makeText(this, toastMessage, Toast.LENGTH_LONG).show();
        } catch (SQLException e) {
            Log.e(LOG_TAG, "Failed to query event", e);
        } catch (SecurityException e) {
            Log.e(LOG_TAG, "Failed to query event", e);
        } catch (IllegalArgumentException e) {
            Log.e(LOG_TAG, "Failed to query event", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    /**
     * Called when extension and sources has been successfully registered.
     * Override this method to take action after a successful registration.
     */
    @Override
    public void onRegisterResult(boolean result) {
        super.onRegisterResult(result);
        Log.d(LOG_TAG, "onRegisterResult");

        // Start adding data if extension is active in preferences
        if (result) {
            SharedPreferences prefs = PreferenceManager
                    .getDefaultSharedPreferences(this);
        }
    }
    
    @Override
    protected RegistrationInformation getRegistrationInformation() {
        return new SWRegistrationInformation(this);
    }

    /*
     * (non-Javadoc)
     * @see com.sonyericsson.extras.liveware.aef.util.ExtensionService#
     * keepRunningWhenConnected()
     */
    @Override
    protected boolean keepRunningWhenConnected() {
        return false;
    }
}
