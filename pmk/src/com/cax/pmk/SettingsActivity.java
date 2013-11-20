package com.cax.pmk;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.widget.TextView;

public class SettingsActivity extends PreferenceActivity {

	public static final String PREFERENCE_VIBRATE          = "pref_vibrate";
	public static final String PREFERENCE_SCREEN_ALWAYS_ON = "pref_screen_always_on";
	
	@SuppressWarnings("deprecation")
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.layout.preferences);
        
	}

    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {

   	 if (preference.getKey().equals("about")) {           
   	       aboutDialog();
   	       return true;
   	 }

   	 return false;
   	}

	private void aboutDialog() {
		AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
 
		// set title
		alertDialogBuilder.setTitle("About");
 
		String versionName = "";
		try {
			versionName = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
		} catch (NameNotFoundException e) {
		    e.printStackTrace();
		}
		    
		// set dialog message
		alertDialogBuilder.setMessage(""
			 + "\"Electronica MK 61\" emulator\n"
			 + "\n"
			 + "App developer:\n"
			 + "Stanislav Borutsky\n"
			 + "stanislavb@gmail.com\n"
			 + "\n"
			 + "Version:\n"
			 + versionName + "\n"
			 + "\n"
			 + "Based on emu145 project by Felix Lazarev\n"
			 + "http://code.google.com/p/emu145\n"
		);

		alertDialogBuilder.setNegativeButton("Close", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
					dialog.cancel();
			}
		});

		// create alert dialog
		AlertDialog alertDialog = alertDialogBuilder.create();
 		 
		// show it
		alertDialog.show();
 
		// change font size
		TextView textView = (TextView) alertDialog.findViewById(android.R.id.message);
		textView.setTextSize(12);

	}
}