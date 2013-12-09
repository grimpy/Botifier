package com.github.grimpy.botifier.preference;

import android.content.Context;
import android.preference.SwitchPreference;
import android.util.AttributeSet;

/**
 * Created by Jo on 12/13/13.
 */
public class PreferenceSwitch extends SwitchPreference {
    public boolean beenClicked = false;

    public PreferenceSwitch(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public PreferenceSwitch(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public PreferenceSwitch(Context context) {
        super(context);
    }

    @Override
    protected void onClick() {
        beenClicked = true;
    }
}
