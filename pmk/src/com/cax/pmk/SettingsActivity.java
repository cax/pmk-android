package com.cax.pmk;
import android.os.Bundle;
import android.preference.PreferenceActivity;

public class SettingsActivity extends PreferenceActivity {

	static final String PREFERENCE_VIBRATE          = "pref_vibrate";
	static final String PREFERENCE_SCREEN_ALWAYS_ON = "pref_screen_always_on";
	static final String ANGLE_MODE_PREFERENCE_KEY   = "angleMode";
	static final String SPEED_MODE_PREFERENCE_KEY   = "speedMode";
	static final String MK_MODEL_PREFERENCE_KEY     = "mkModel";
	
	@SuppressWarnings("deprecation")
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.layout.preferences);
        
	}

	/* // method that implements clicking callback in preferences screen
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {

   	 if (preference.getKey().equals("about")) {           
   	       aboutDialog();
   	       return true;
   	 }

   	 return false;
   	}
	 */

}