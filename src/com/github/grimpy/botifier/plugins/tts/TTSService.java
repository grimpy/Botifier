package com.github.grimpy.botifier.plugins.tts;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.speech.tts.TextToSpeech;
import android.telephony.TelephonyManager;
import android.text.TextUtils;

import com.github.grimpy.botifier.plugins.AbstractPlugin;
import com.github.grimpy.botifier.Botification;
import com.github.grimpy.botifier.R;

public class TTSService extends AbstractPlugin implements TextToSpeech.OnInitListener{

    private TextToSpeech mTTS;
    private SharedPreferences mSharedPref;
    private TelephonyManager mTelephonyManager;
    private AudioManager mAudioManager;
    private String mActiveBotifaction;

    @Override
    public void onInit(int i) {
        //needed for tts
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mTTS = new TextToSpeech(this, this);
        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        mTelephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        mSharedPref = PreferenceManager.getDefaultSharedPreferences(this);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void notificationAdded(Botification bot) {
        if (mSharedPref.getBoolean(_(R.string.pref_tts_enabled), false) &&
                mTelephonyManager.getCallState() == TelephonyManager.CALL_STATE_IDLE &&
                (mAudioManager.isBluetoothA2dpOn() || !mSharedPref.getBoolean(_(R.string.pref_tts_bt_only), true))) {
            String txt = bot.getPreference(_(R.string.pref_tts_value), true);
            mActiveBotifaction = bot.getID();
            mTTS.speak(txt, TextToSpeech.QUEUE_FLUSH, null);
        }
    }

    @Override
    public void notificationRemoved(Botification bot) {
        if (TextUtils.equals(bot.getID(), mActiveBotifaction)) {
            mTTS.stop();
        }
    }
}