package com.github.grimpy.botifier.preference;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.ColorFilter;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.drawable.Drawable;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.github.grimpy.botifier.R;

public class ApplicationFilterFragment extends PreferenceFragment {
	private SharedPreferences mSharedPref;
	private PreferenceCategory mBlackList;
	private Set<String> mBlackListEntries;
	private PackageManager mPackageManager;
	private ColorFilter mGrayscaleFilter;
	
	class AppPreference extends CheckBoxPreference  {
		private String mPkgName; 

		public String getPkgName() {
			return mPkgName;
		}

		public void setPkgName(String mPkgName) {
			this.mPkgName = mPkgName;
		}

		public AppPreference(Context context) {
			super(context);
		}
		
	}
	class ApplicationComparator implements Comparator<AppPreference> {
	    @Override
	    public int compare(AppPreference a, AppPreference b) {
            	return a.getTitle().toString().toLowerCase().compareTo(b.getTitle().toString().toLowerCase());
	    }
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
		mSharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity());
		mPackageManager = getActivity().getPackageManager();
        ColorMatrix colorMatrix = new ColorMatrix();
        colorMatrix.setSaturation(0f);
        float[] matrix = colorMatrix.getArray();
        matrix[18] = 0.5f;
        mGrayscaleFilter = new ColorMatrixColorFilter(colorMatrix);
		addPreferencesFromResource(R.xml.list_preference);
		mBlackList = (PreferenceCategory) findPreference(getString(R.string.cat_filterlist));
		mBlackList.setTitle(R.string.applications);
		Set<String> entries = mSharedPref.getStringSet(getString(R.string.pref_blocked_applist), null);
		if (entries == null) {
			mBlackListEntries = new HashSet<String>();
		} else {
			mBlackListEntries = new HashSet<String>(entries);
		}
		List<ApplicationInfo> pkgs = mPackageManager.getInstalledApplications(PackageManager.GET_META_DATA);
		List<AppPreference> prefs = new ArrayList<ApplicationFilterFragment.AppPreference>();
		
		for (ApplicationInfo pkg: pkgs) {
			AppPreference pref = new AppPreference(getActivity());
			pref.setTitle(mPackageManager.getApplicationLabel(pkg));
			Drawable icon = pkg.loadIcon(mPackageManager);
			pref.setPkgName(pkg.packageName);
			if (mBlackListEntries.contains(pkg.packageName)) {
				pref.setDefaultValue(false);
                icon.setColorFilter(mGrayscaleFilter);
            } else {
				pref.setDefaultValue(true);
			}
			pref.setIcon(icon);
			prefs.add(pref);
		}
		Collections.sort(prefs, new ApplicationComparator());
		for (AppPreference pref: prefs) {
			mBlackList.addPreference(pref);
		}
	}

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        MenuItem item = menu.add(R.string.select_all);
        item.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        item = menu.add(R.string.select_none);
        item.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int amount = mBlackList.getPreferenceCount();
        boolean selected = item.getTitle().equals(getString(R.string.select_all));
        for (int i=0; i < amount; i++) {
            AppPreference pref = (AppPreference) mBlackList.getPreference(i);
            pref.setChecked(selected);
            editEntry(pref);
        }
        return super.onOptionsItemSelected(item);
    }

	private void editEntry(AppPreference pref) {

        String pkg = pref.getPkgName();
        boolean disabled = !pref.isChecked();
        Drawable icon = pref.getIcon();
        if (pref.isChecked()) {
            icon.setColorFilter(null);
        } else {
            icon.setColorFilter(mGrayscaleFilter);
        }
		ArrayList<String> newlist = new ArrayList<String>(mBlackListEntries);
		boolean isblacklisted = newlist.contains(pkg);
		if (disabled && isblacklisted) {
			return;
		} else if (disabled) {
			newlist.add(pkg);
		} else if (!disabled && !isblacklisted){
			return;
		} else if (!disabled) {
			newlist.remove(pkg);
		}
		mBlackListEntries = new HashSet<String>(newlist);
		Editor editor = mSharedPref.edit();
        editor.putStringSet(getString(R.string.pref_blocked_applist), mBlackListEntries);
        editor.apply();
	}

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, final Preference preference) {
        if (!isAdded()) {
            return false;
        }
		AppPreference pref = (AppPreference) preference;
		editEntry(pref);
		return true;
	}    
}
