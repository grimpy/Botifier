package com.github.grimpy.botifier;

import java.util.ArrayList;
import java.util.Set;
import java.util.regex.Pattern;

import android.app.Notification;
import android.app.Service;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RemoteViews;
import android.widget.TextView;


public class Botification implements Parcelable {
	private Service mService;
	public String mPackageLabel;
	public String mDescription;
	public String mText;
	public String mPkg;
	public int mId;
	private SharedPreferences mSharedPref;
    
	public int mOffset;
	public String mTag;

	private static String TAG = "Botifier";
	private static final int TIMESTAMPID = 16908388;
	
	
    public static final Parcelable.Creator<Botification> CREATOR = new Parcelable.Creator<Botification>() {
        public Botification createFromParcel(Parcel in) {
        	int id = in.readInt();
        	String pkg = in.readString();
        	String tag = in.readString();
        	String description = in.readString();
        	String text = in.readString();
            return new Botification(id, pkg, tag, description, text);
        }

		@Override
		public Botification[] newArray(int size) {
			return new Botification[size];
		}

    };
    
	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeInt(mId);
		dest.writeString(mPkg);		
		dest.writeString(mTag);
		dest.writeString(mDescription);
		dest.writeString(mText);
	}
	
	public Botification(int id, String pkg, String tag, String description, String text) {
		mId = id;
		mPkg = pkg;
		mTag = tag;
		mOffset = 0;
		mDescription = description;
		mText = text;
	}
	
	private String getPackageLabel(Service service, String packagename){
		PackageManager packageManager = service.getPackageManager();
		ApplicationInfo ai;
		try {
		    ai = packageManager.getApplicationInfo( packagename, 0);
		} catch (final NameNotFoundException e) {
		    ai = null;
		}
		return (String) (ai != null ? packageManager.getApplicationLabel(ai) : packagename);

	}

	
	public void load(Service service) {
		mService = service;
		mPackageLabel = getPackageLabel(service, mPkg);
		mSharedPref = PreferenceManager.getDefaultSharedPreferences(service);
	    
	}
	
	private int getMaxLength() {
		String maxLength = mSharedPref.getString(mService.getString(R.string.pref_maxlength), "");
		if (!TextUtils.isEmpty(maxLength)){
			return Integer.valueOf(maxLength);
		}
		return 0;
	}
	
    private String _(int id){
        return mService.getString(id);
    }
	
    public boolean isBlackListed() {
        String txt = mText;
        Set<String> blacklist = mSharedPref.getStringSet(_(R.string.pref_blacklist), null);
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
        Set<String> appblacklist = mSharedPref.getStringSet(_(R.string.pref_blocked_applist), null);
        if (appblacklist != null) {
            for (String entry : appblacklist) {
                if (entry.equals(mPkg)) {
                    return true;
                }
            }
        }
        
        return false;
    }


    public String getID(){
        return String.format("%s.%s", mPkg, mId);
    }

    public boolean isIntresting(Notification not) {
        boolean isongoing = (not.flags & Notification.FLAG_ONGOING_EVENT) != 0;
        boolean wantongoing = mSharedPref.getBoolean(_(R.string.pref_persistent_notification), false);
        return (wantongoing || !isongoing);
    }

	public boolean hasNext() {
		int maxlength = getMaxLength();
		if (maxlength == 0) {
			return false;
		}
		return (mOffset+1)*maxlength < mText.length();
	}
	
	public String getPreference(String key) {
		return getPreference(key, false);
	}
	
	public String getPreference(String key, boolean full) {
		String message = mSharedPref.getString(key, "");
		int maxlength = getMaxLength();
		message = message.replace("%f", toString());
		message = message.replace("%a", mPackageLabel);
		message = message.replace("%d", mDescription);
		message = message.replace("%m", mText);
		
		if (!full && key.equals(mService.getString(R.string.pref_metadata_title))) {
			if (maxlength != 0 && message.length() > maxlength) {
				int start = mOffset * maxlength;
				int end = start + maxlength;
				if (end >= message.length()) {
					end = message.length() -1;
					mOffset = -1;
				}
				String result = message.substring(start, end);
				mOffset++;
				return result;
			}
		}
		return message;
	}
	
	public String toString() {
		return String.format("%s %s %s", mPackageLabel, mDescription, mText);
	}

	@Override
	public boolean equals(Object o) {
		if (Botification.class.isInstance(o)) {
			Botification not = (Botification) o;
            if (!TextUtils.equals(mTag, not.mTag)) {
                return false;
            }
            if (!TextUtils.equals(mPkg, not.mPkg)) {
                return false;
            }
            if (mId != not.mId) {
                return false;
            }
		}
		return true;
	}
	
    private static void extractViewType(ArrayList<View> outViews, Class<TextView> viewtype, View source) {
    	if (ViewGroup.class.isInstance(source)) {
    		ViewGroup vg = (ViewGroup) source;
    		for (int i = 0; i < vg.getChildCount(); i++) {
    			extractViewType(outViews, viewtype, vg.getChildAt(i));
				
			}
    	} else if(viewtype.isInstance(source)) {
			outViews.add(source);
    	}
    }
    
    public static String extractTextFromNotification(Service service, Notification notification) {
    	ArrayList<String> result = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            result =  extractTextFromNotification(service, notification.bigContentView);
        }
	    if (result == null) {
	    	result = extractTextFromNotification(service, notification.contentView);
	    }
	    if (result == null){
	    	return "";
	    }
	    return TextUtils.join("\n", result);

    }
    
    private static ArrayList<String> extractTextFromNotification(Service service, RemoteViews view) {
    	LayoutInflater inflater = (LayoutInflater) service.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	    ArrayList<String> result = new ArrayList<String>();
	    if (view == null) {
	    	Log.d(TAG, "View is empty");
	    	return null;
	    }
		try {
			int layoutid = view.getLayoutId();
			ViewGroup localView = (ViewGroup) inflater.inflate(layoutid, null);
		    view.reapply(service.getApplicationContext(), localView);
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
			Log.d(TAG, "FAILED to load notification " + e.toString());
			Log.wtf(TAG, e);
			return null;
			//notification might have dissapeared by now
		}
		Log.d(TAG, "Return result" + result);
	    return result;
    }

	@Override
	public int describeContents() {
		return 0;
	}
}
