package com.cax.pmk;

import java.util.ArrayList;
import java.util.List;

import android.os.Bundle;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.util.Log;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

public class MainActivity extends Activity {

	private static final String EMPTY_INDICATOR = "/ / / / / / / / / / / / //"; // slash is an empty comma placeholder in indicator font

	private static final int [] blackButtons = 
			new int[] { R.id.buttonStepBack,	R.id.buttonStepForward, R.id.buttonReturn,	R.id.buttonStopStart,
		  				R.id.buttonXToRegister,	R.id.buttonRegisterToX, R.id.buttonGoto,	R.id.buttonSubroutine};

	private EmulatorInterface emulator = null; void setEmulator(EmulatorInterface emulator) { this.emulator = emulator; }
	private int angleMode = 0;
	private int speedMode = 0;
	private int mkModel = 0; // 0 for MK-61, 1 for MK-54

	private int yellowLabelLeftPadding = 0;
	private float buttonTextSize = 0;
	private float labelTextSize = 0;
	private boolean  vibrate = true;
	private boolean  vibrateWithMoreIntensity = false;
	private int calcNameTouchCounter = 0;
	
	private TextView calculatorIndicator = null;
	
    private CheckBox powerOnOffCheckBox = null;
    private SeekBar  powerOnOffSlider   = null;

    private RadioButton radioRadians = null;
    private RadioButton radioGrads 	 = null;
    private RadioButton radioDegrees = null;
    private SeekBar angleModeSlider  = null;
    
    private int poweredOn = 0;
	private Vibrator vibrator = null;
	
	SaveStateManager saveStateManager = new SaveStateManager(this);
	
	static boolean splashScreenMode = false;
	
    // ----------------------- Activity life cycle handlers --------------------------------
	@Override
    public void onCreate(Bundle savedInstanceState) {
      	Log.d("DDD", "onCreate entered");

		super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main_mk_61);

    	MenuHelper.mainActivity = this;

        // remember switches controls
        powerOnOffSlider 	= (SeekBar) findViewById(R.id.powerOnOffSlider); 
        powerOnOffCheckBox	= (CheckBox)findViewById(R.id.powerOnOffCheckBox); 

        angleModeSlider	= (SeekBar) findViewById(R.id.angleModeSlider);
        radioRadians 	= (RadioButton) findViewById(R.id.radioRadians);
        radioGrads 	 	= (RadioButton) findViewById(R.id.radioGrads);
        radioDegrees 	= (RadioButton) findViewById(R.id.radioDegrees);

        // remember labels padding for later use
        yellowLabelLeftPadding = findViewById(R.id.label10powerX).getPaddingLeft();
        
        // remember button and label text size for later use
	    buttonTextSize = ((Button)  findViewById(R.id.buttonF    )).getTextSize();
	    labelTextSize  = ((TextView)findViewById(R.id.labelSquare)).getTextSize();

        // remember vibrator service
        vibrator = (Vibrator) this.getSystemService(VIBRATOR_SERVICE);
        
        // style indicator
        calculatorIndicator = (TextView) findViewById(R.id.textView_Indicator);
        Typeface tf = Typeface.createFromAsset(this.getAssets(), "fonts/digital-7-mod.ttf");
        calculatorIndicator.setTypeface(tf);
        calculatorIndicator.setText(EMPTY_INDICATOR); // let AutoScaleTextView do the work - set font size and fix layout

        // use manually created symbols for some labels
        tf = Typeface.createFromAsset(this.getAssets(), "fonts/missing-symbols.ttf");
        for (int viewId : new int[] { R.id.labelSquare, R.id.labelEpowerX, R.id.label10powerX, R.id.labelXpowerY, R.id.labelDot }) {
        	((TextView)findViewById(viewId)).setTypeface(tf);
        }
        
        // use manually created symbols for some buttons
        for (int viewId : new int[] { R.id.buttonUpStack, 		R.id.buttonStepBack, 	R.id.buttonStepForward, 
        							  R.id.buttonRegisterToX, 	R.id.buttonXToRegister, R.id.buttonExchangeXY }) {
        	((Button)findViewById(viewId)).setTypeface(tf, Typeface.NORMAL);
        }
        
        // preferences initialization
        PreferenceManager.setDefaultValues(this, R.layout.preferences, false);
        activateSettings();
        
        // recover speed and angle modes from preferences even if calculator was switched off before destroying
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        speedMode = sharedPref.getInt(SettingsActivity.SPEED_MODE_PREFERENCE_KEY, SettingsActivity.DEFAULT_SPEED_MODE);
        setAngleModeControl(sharedPref.getInt(SettingsActivity.ANGLE_MODE_PREFERENCE_KEY, SettingsActivity.DEFAULT_ANGLE_MODE));
        
        // set listeners for slider movement
        angleModeSlider.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) { if (fromUser) onAngleMode(progress); }
        });
        
        powerOnOffSlider.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {} 
            @Override public void onProgressChanged(final SeekBar seekBar, int progress, boolean fromUser) { if (fromUser) onPower(progress); }   	
        });
                
        // recover and set mkModel
        setMkModel(sharedPref.getInt(SettingsActivity.MK_MODEL_PREFERENCE_KEY, SettingsActivity.DEFAULT_MK_MODEL));
        
        // Workaround for broken layout that fixes itself after refresh:
        // start activity that does nothing and returns immediately
       	splashScreenMode = true; // tells onPause/onRelease to their work only once, not twice
      	Log.d("DDD", "onCreate set splashScreenMode, starting activity");
        startActivity(new Intent(getApplicationContext(), SplashScreenActivity.class));
      	Log.d("DDD", "onCreate leaving");
        	
	}

	@Override
    public void onDestroy() {
		// remember speed mode, angle mode and mk model even if calculator was switched off before destroying
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
    	
        SharedPreferences.Editor editor = sharedPref.edit();
    	editor.putInt(SettingsActivity.SPEED_MODE_PREFERENCE_KEY, speedMode);
    	editor.putInt(SettingsActivity.ANGLE_MODE_PREFERENCE_KEY, angleMode);
    	editor.putInt(SettingsActivity.MK_MODEL_PREFERENCE_KEY,   mkModel);
    	editor.commit();
    	
    	super.onDestroy();
	}
	
    @Override
    public void onPause() {
    	super.onPause();
    	if (splashScreenMode) {
    		splashScreenMode = false;
    		return;
    	}
    	
       	saveStateManager.saveStateStoppingEmulator(emulator, -1); // save persistence emulation state
       	emulator = null;
    }

    @Override
    public void onResume() {
    	super.onResume();
    	if (splashScreenMode) {
    		return;
    	}
    	
    	activateSettings();
      	if (emulator == null) saveStateManager.loadState(emulator, -1); // load persistence emulation state
    }
    
    // ----------------------- Menu hooks --------------------------------
    public boolean onPrepareOptionsMenu(Menu menu)
    {
        MenuItem menu_save = menu.findItem(R.id.menu_save);      
        MenuItem menu_swap = menu.findItem(R.id.menu_swap_model);      

        if(poweredOn == 1) 
        {           
        	menu_swap.setVisible(false);
        	menu_save.setVisible(true);
        }
        else
        {
        	menu_swap.setVisible(true);
        	menu_save.setVisible(false);
        }
        return true;
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	 switch (item.getItemId()) {
	    	 case R.id.menu_about:
	    		 MenuHelper.aboutDialog();
	 	        return true;
	    	 case R.id.menu_settings:
	    		 MenuHelper.goSettingsScreen();
    	        return true;
    	    case R.id.menu_swap_model:
    	    	MenuHelper.onChooseMkModel(mkModel);
    	        return true;
    	    case R.id.menu_save:
    	    	saveStateManager.chooseAndUseSaveSlot(emulator, true);
    	        return true;
    	    case R.id.menu_load:
    	    	saveStateManager.chooseAndUseSaveSlot(emulator, false);
    	        return true;
    	        
    	    default:
    	        return super.onOptionsItemSelected(item);
    	    }
    }
    
    // ----------------------- Setting controls state --------------------------------
    void setAngleModeControl(int mode) {
    	angleMode = mode;
    	angleModeSlider.setProgress(angleMode);
    	radioRadians.setChecked(angleMode == 0);
    	radioGrads  .setChecked(angleMode == 1);
    	radioDegrees.setChecked(angleMode == 2);
    }
    
    void setPowerOnOffControl(int mode) {
        poweredOn = mode;
        if (powerOnOffSlider.getProgress() != mode) powerOnOffSlider.setProgress(mode);
        if ((powerOnOffCheckBox.isChecked() ? 1:0) != mode) powerOnOffCheckBox.setChecked(mode==1);
    }

    void setIndicatorColor(int mode) {
    	String color;
    	if (mode < 0) {
    		color = SettingsActivity.INDCATOR_OFF_COLOR;
    	}
    	else {
        	speedMode = mode;
			color = (speedMode == 0)
    				? SettingsActivity.INDCATOR_FAST_SPEED_COLOR 
    				: SettingsActivity.INDCATOR_SLOW_SPEED_COLOR;
    	}

    	((LinearLayout)findViewById(R.id.linearLayout_Indicator))
    			.setBackgroundColor(Color.parseColor(color));
    }
    
    // Show string on calculator display 
    // Not really a callback - called also from Emulator thread  
    public void setDisplay(final String text) {
    	runOnUiThread(new Runnable() {
    	   public void run() {
    		   if (calculatorIndicator != null)
    			   calculatorIndicator.setText(text);
    	   }
    	});
    }
    
    // ----------------------- UI call backs --------------------------------
    // calculator name touch callback
    public void onCalculatorNameTouched(View view) {
    	calcNameTouchCounter = (calcNameTouchCounter + 1) % 33;
    	if (calcNameTouchCounter != 0) return; // act only on every 33th click - hidden feature !
    	
    	setMkModel(1 - mkModel);
    	if (emulator != null)
    		emulator.setMkModel(mkModel);
    }

    // calculator indicator touch callback
    public void onIndicatorTouched(View view) {
        if (emulator != null) {
        	emulator.setSpeedMode(1 - emulator.getSpeedMode());
        	setIndicatorColor(emulator.getSpeedMode());
        }
    }

    // calculator power switch callback
    public void onPowerCheckBoxTouched(View view) {
    	onPower(((CheckBox)view).isChecked() ? 1 : 0);
    }
    
    private void onPower(int progress) {
    	if (poweredOn == progress)
    		return;
    	poweredOn = progress;
    	if (vibrate) vibrator.vibrate(SettingsActivity.VIBRATE_ON_OFF_SWITCH);
    	switchOnCalculator(poweredOn == 1);
    }
    
    // calculator angle mode switch callback
    public void onAngleModeRadioButtonTouched(View view) {
    	onAngleMode(Integer.parseInt((String)view.getTag()));
    }
    
    private void onAngleMode(int progress) {
    	angleMode = progress;
        if (emulator != null) {
        	emulator.setAngleMode(angleMode);
        	if (vibrate) vibrator.vibrate(SettingsActivity.VIBRATE_ANGLE_SWITCH);
        }
    }

    // calculator button press callback
    public void onKeypadButtonTouched(View view) {
    	if (emulator == null)
    		return;
    	
    	int keycode = Integer.parseInt((String)view.getTag());
    	emulator.keypad(keycode);
    	if (vibrate) vibrator.vibrate(vibrateWithMoreIntensity ? SettingsActivity.VIBRATE_KEYPAD_MORE : SettingsActivity.VIBRATE_KEYPAD);
    }
    
    // ----------------------- Other --------------------------------
    void styleButtons() {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
    	
        float chosenButtonTextSize = buttonTextSize * Float.parseFloat(sharedPref.getString("pref_button_text_size", "1"));
        float chosenLabelTextSize  = labelTextSize  * Float.parseFloat(sharedPref.getString("pref_label_text_size", "1"));

    	boolean borderBlackButtons = sharedPref.getBoolean(SettingsActivity.PREFERENCE_BORDER_BLACK_BUTTONS, SettingsActivity.DEFAULT_BORDER_BLACK_BUTTONS);
    	boolean borderOtherButtons = sharedPref.getBoolean(SettingsActivity.PREFERENCE_BORDER_OTHER_BUTTONS, SettingsActivity.DEFAULT_BORDER_OTHER_BUTTONS);
    	
    	float smallerButtonTextSize = (float) (chosenButtonTextSize * 0.8);
	    ((Button)findViewById(R.id.buttonReturn   )).setTextSize(TypedValue.COMPLEX_UNIT_PX, smallerButtonTextSize);
	    ((Button)findViewById(R.id.buttonStopStart)).setTextSize(TypedValue.COMPLEX_UNIT_PX, smallerButtonTextSize);

	    List<View> list = getAllChildrenBFS(findViewById(R.id.tableLayoutKeyboard));
	    for (int i=0; i < list.size(); i++) {
	    	if (list.get(i) instanceof Button) {
	    		Button b = (Button)list.get(i);
	    		b.setTextSize(TypedValue.COMPLEX_UNIT_PX, chosenButtonTextSize);
	    		boolean isBlack = false;
	    		for (int j=0; j < blackButtons.length; j++) if (b.getId() == blackButtons[j]) { isBlack = true; break; }
	    		if (isBlack) {
	    			b.setBackgroundResource(borderBlackButtons ? R.drawable.button_black_border_white : R.drawable.button_black);
	    		} else {
	    			if (b.getId() == R.id.buttonF)
		    			b.setBackgroundResource(borderOtherButtons ? R.drawable.button_yellow_border_black	: R.drawable.button_yellow);
	    			else if (b.getId() == R.id.buttonK)
	    				b.setBackgroundResource(borderOtherButtons ? R.drawable.button_blue_border_black	: R.drawable.button_blue);
	    			else if (b.getId() == R.id.buttonClear)
	    				b.setBackgroundResource(borderOtherButtons ? R.drawable.button_red_border_black		: R.drawable.button_red);
	    			else 
	    				b.setBackgroundResource(borderOtherButtons ? R.drawable.button_other_border_black	: R.drawable.button_other);
	    		}
	    	} else if (list.get(i) instanceof TextView) {
	    		((TextView)list.get(i)).setTextSize(TypedValue.COMPLEX_UNIT_PX, chosenLabelTextSize);
	    	}
	    }
    }
    
    private List<View> getAllChildrenBFS(View v) {
        List<View> visited = new ArrayList<View>();
        List<View> unvisited = new ArrayList<View>();
        unvisited.add(v);

        while (!unvisited.isEmpty()) {
            View child = unvisited.remove(0);
            visited.add(child);
            if (!(child instanceof ViewGroup)) continue;
            ViewGroup group = (ViewGroup) child;
            final int childCount = group.getChildCount();
            for (int i=0; i<childCount; i++) unvisited.add(group.getChildAt(i));
        }

        return visited;
    }
    
    private static final int[] blueLabels = {
		R.id.labelFloor,	R.id.labelFrac,	R.id.labelMax, 
		R.id.labelAbs,		R.id.labelSign,	R.id.labelFromHM,	R.id.labelToHM,
											R.id.labelFromHMS,	R.id.labelToHMS,R.id.labelRandom,
		R.id.labelNOP,		R.id.labelAnd,	R.id.labelOr,		R.id.labelXor,	R.id.labelInv
	};
	
	private static final CharSequence[] blueLabelsText = new CharSequence[blueLabels.length];
    
	private static final int[] pairedYellowLabels = {
		R.id.labelSin,		R.id.labelCos,		R.id.labelTg,
		R.id.labelArcSin,	R.id.labelArcCos,	R.id.labelArcTg,	R.id.labelPi,
												R.id.labelLn,		R.id.labelXpowerY,	R.id.labelBx,
		R.id.label10powerX,	R.id.labelDot,		R.id.labelAVT,		R.id.labelPRG,		R.id.labelCF
	};

	void setMkModel(int mkModel) {
		boolean doNothing = false;
		if (mkModel == this.mkModel)
			doNothing = true;

        ((TextView) findViewById(R.id.TextViewTableCellCalculatorName))
    	.setText(getString(R.string.electronica) + "  MK" + (mkModel==1 ? "-54" : " 61"));

        if (doNothing) return;
        
    	if (mkModel == 1) { // 1 for MK-54, 0 for MK-61
    		
    		for (int i=0; i < blueLabels.length; i++) {

	    		View modView = (TextView) findViewById(pairedYellowLabels[i]);
	    		modView.setPadding(0, 0, 0, 0);

		    	LayoutParams params = (LayoutParams) modView.getLayoutParams();
		    	params.addRule(RelativeLayout.ALIGN_PARENT_LEFT, 0);
		    	params.addRule(RelativeLayout.CENTER_IN_PARENT);

		    	findViewById(pairedYellowLabels[i]).setPadding(0, 0, 0, 0);

		    	TextView blueLabel = (TextView) findViewById(blueLabels[i]);
		    	blueLabelsText[i] = blueLabel.getText(); 
		    	blueLabel.setText("");
    		}
		    
    		((TextView) findViewById(R.id.labelE)).setText("");
    		((TextView) findViewById(R.id.labelNop54)).setText("НОП");
    		((Button)findViewById(R.id.buttonClear)).setText("Cx");
    		
    	} else {

    		for (int i=0; i < blueLabels.length; i++) {
	    		View modView = (TextView) findViewById(pairedYellowLabels[i]);
	    		modView.setPadding(yellowLabelLeftPadding, 0, 0, 0);

		    	LayoutParams params = (LayoutParams) modView.getLayoutParams();
		    	params.addRule(RelativeLayout.CENTER_IN_PARENT, 0);
		    	params.addRule(RelativeLayout.ALIGN_PARENT_LEFT);


		    	TextView blueLabel = (TextView) findViewById(blueLabels[i]);
		    	blueLabel.setText(blueLabelsText[i]);
    		}

    		((TextView) findViewById(R.id.labelE)).setText("e");
    		((TextView) findViewById(R.id.labelNop54)).setText("");
    		((Button)findViewById(R.id.buttonClear)).setText("CX");
    	}
    	
    	this.mkModel = mkModel;
    }
		
    private void activateSettings() {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        vibrate = sharedPref.getBoolean(SettingsActivity.PREFERENCE_VIBRATE, 
        								SettingsActivity.DEFAULT_VIBRATE);
        vibrateWithMoreIntensity = sharedPref.getBoolean(SettingsActivity.PREFERENCE_VIBRATE_KEYPAD_MORE,
        												 SettingsActivity.DEFAULT_VIBRATE_KEYPAD_MORE);
  		calculatorIndicator.setKeepScreenOn(
  				sharedPref.getBoolean(SettingsActivity.PREFERENCE_SCREEN_ALWAYS_ON,
  									  SettingsActivity.DEFAULT_SCREEN_ALWAYS_ON));
  		
        boolean sliderOnOff = sharedPref.getBoolean(SettingsActivity.PREFERENCE_SLIDER_ON_OFF, 
													SettingsActivity.DEFAULT_SLIDER_ON_OFF);
        powerOnOffSlider  .setVisibility(sliderOnOff ? View.VISIBLE : View.GONE);
        powerOnOffCheckBox.setVisibility(sliderOnOff ? View.GONE    : View.VISIBLE);

        boolean sliderAngle = sharedPref.getBoolean(SettingsActivity.PREFERENCE_SLIDER_ANGLE, 
													SettingsActivity.DEFAULT_SLIDER_ANGLE);
        angleModeSlider.setVisibility(sliderAngle ? View.VISIBLE : View.GONE);
        radioRadians.setVisibility(sliderAngle ? View.GONE : View.VISIBLE);
        radioGrads  .setVisibility(sliderAngle ? View.GONE : View.VISIBLE);
        radioDegrees.setVisibility(sliderAngle ? View.GONE : View.VISIBLE);
        
  		// scale buttons text and set borders
        styleButtons();
    }
        
    private void switchOnCalculator(boolean enable) {
    	if (enable) {
    		if (poweredOn == 1) {
	            emulator = new com.cax.pmk.emulator.Emulator();
	    		emulator.setAngleMode(angleMode);
	    		emulator.setSpeedMode(speedMode);
	    		emulator.setMkModel(mkModel);
	    		emulator.initTransient(this);
	        	setIndicatorColor(speedMode);
	            emulator.start();
    		}
    	} else {
    		if (emulator != null) {
    			emulator.stopEmulator();
    			emulator = null;
    		}
            calculatorIndicator.setText(EMPTY_INDICATOR);

            // just in case...
            setPowerOnOffControl(0);
            
            //erase persistence file
            saveStateManager.deleteSlot(-1);

            setIndicatorColor(-1);
    	}
    }
}
