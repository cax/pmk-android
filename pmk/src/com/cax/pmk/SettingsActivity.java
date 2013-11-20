package com.cax.pmk;
import android.os.Bundle;
import android.preference.PreferenceActivity;

public class SettingsActivity extends PreferenceActivity {

	static final String PREFERENCE_SCREEN_ALWAYS_ON 	= "pref_screen_always_on";
	static final String PREFERENCE_SLIDER_ON_OFF		= "pref_slider_on_off";
	static final String PREFERENCE_SLIDER_ANGLE			= "pref_slider_angle";
	static final String PREFERENCE_VIBRATE          	= "pref_vibrate";
	static final String PREFERENCE_VIBRATE_KEYPAD_MORE	= "pref_vibrate_keypad_more";
	static final String PREFERENCE_BORDER_BLACK_BUTTONS = "pref_border_black_buttons";
	static final String PREFERENCE_BORDER_OTHER_BUTTONS = "pref_border_other_buttons";
	
	static final String ANGLE_MODE_PREFERENCE_KEY   = "angleMode";
	static final String SPEED_MODE_PREFERENCE_KEY   = "speedMode";
	static final String MK_MODEL_PREFERENCE_KEY     = "mkModel";
	
	static final boolean DEFAULT_SCREEN_ALWAYS_ON		= false;
	static final boolean DEFAULT_SLIDER_ON_OFF			= true;
	static final boolean DEFAULT_SLIDER_ANGLE			= true;
	static final boolean DEFAULT_VIBRATE 				= true;
	static final boolean DEFAULT_VIBRATE_KEYPAD_MORE	= false;
	static final boolean DEFAULT_BORDER_BLACK_BUTTONS	= true;
	static final boolean DEFAULT_BORDER_OTHER_BUTTONS	= true;

	static final int     DEFAULT_ANGLE_MODE  		= 0; // radians
	static final int     DEFAULT_SPEED_MODE  		= 1; // slow
	static final int     DEFAULT_MK_MODEL    		= 0; // MK 61
	
	static final String INDCATOR_OFF_COLOR 		  = "#000000";
	static final String INDCATOR_FAST_SPEED_COLOR = "#255525";
	static final String INDCATOR_SLOW_SPEED_COLOR = "#103010";
	
	static final int    VIBRATE_ON_OFF_SWITCH = 50;
	static final int    VIBRATE_ANGLE_SWITCH  = 20;
	static final int    VIBRATE_KEYPAD 		  = 15;
	static final int    VIBRATE_KEYPAD_MORE	  = 25;
	
	@SuppressWarnings("deprecation")
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.layout.preferences);
        
	}
	
/*
	// method that implements clicking callback in preferences screen
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {

   	 if (preference.getKey().equals("pref_screen_always_on")) {           
			// do your stuff
   	       	return true;
   	 }

   	 return false;
   	}
*/

}