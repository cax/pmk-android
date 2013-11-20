package com.cax.pmk;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import android.os.Bundle;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Color;
import android.graphics.LightingColorFilter;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.text.Editable;
import android.text.InputFilter;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.TextView;

public class MainActivity extends Activity {

	private static final String PERSISTENCE_STATE_FILENAME 	= "persist";
    private static final String PERSISTENCE_STATE_EXTENSION = ".pmk";
	private static final String EMPTY_INDICATOR = "/ / / / / / / / / / / / /"; // slash is an empty comma placeholder in indicator font
	
	private static final String INDCATOR_OFF_COLOR 		  = "#000000";
	private static final String INDCATOR_FAST_SPEED_COLOR = "#444444";
	private static final String INDCATOR_SLOW_SPEED_COLOR = "#001500";
	
	private static final int    VIBRATE_ON_OFF_SWITCH = 50;
	private static final int    VIBRATE_ANGLE_SWITCH  = 20;
	private static final int    VIBRATE_KEYPAD 		  = 15;
	
	private static final int    EDIT_TEXT_ENTRY_ID = 12345; // it has to be some number, so let it be 12345
	private static final int    SAVE_SLOTS_NUMBER = 99;
	private static final int    SAVE_NAME_MAX_LEN = 25;

	private EmulatorInterface emulator = null;
	private int selectedSaveSlot = 0;
    private int tempSaveSlot = 0;
	private int angleMode = 0;
	private int speedMode = 0;
	private int mkModel = 0; // 0 for MK-61, 1 for MK-54

	private int yellowLabelLeftPadding = 0;
	private float buttonTextSize = 0;
	private float labelTextSize = 0;
	private boolean  vibrate = true;
	private int calcNameTouchCounter = 0;

	private TextView calculatorIndicator = null;
    private CheckBox powerOnOffCheckbox = null;
	private Vibrator vibrator = null;
	
    // ----------------------- Activity life cycle handlers --------------------------------
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.activity_main_mk_61);

        // remember on/off check box
        powerOnOffCheckbox = (CheckBox)findViewById(R.id.checkBoxPowerOnOff); 

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
        
        // scale buttons text
        scaleButtonsTextSize();
        
        // preferences initialization
        PreferenceManager.setDefaultValues(this, R.layout.preferences, false);
        activateSettings();
        
        // recover speed and angle modes from preferences even if calculator was switched off before destroying
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        speedMode = sharedPref.getInt(SettingsActivity.SPEED_MODE_PREFERENCE_KEY, 0);
        angleMode = sharedPref.getInt(SettingsActivity.ANGLE_MODE_PREFERENCE_KEY, 0);
        setAngleModeRadios();
        
        // recover and set mkModel
        setMkModel(sharedPref.getInt(SettingsActivity.MK_MODEL_PREFERENCE_KEY, 0));
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
    public void onStart() {
    	super.onStart();
    	if (! loadState(-1)) { // load persistence emulation state
    		switchOnCalculator(true);
    	}
    }

    @Override
    public void onStop() {
    	super.onStop();
       	saveState(-1); // load persistence emulation state
    }

    @Override
    public void onResume() {
    	super.onResume();
        activateSettings();
    }
    
    // ----------------------- Menu hooks --------------------------------
    public boolean onPrepareOptionsMenu(Menu menu)
    {
        MenuItem menu_save = menu.findItem(R.id.menu_save);      
        MenuItem menu_swap = menu.findItem(R.id.menu_swap_model);      

        if(powerOnOffCheckbox.isChecked()) 
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
	    		 aboutDialog();
	 	        return true;
	    	 case R.id.menu_settings:
    	    	goSettingsScreen();
    	        return true;
    	    case R.id.menu_swap_model:
    	    	onChooseMkModel();
    	        return true;
    	    case R.id.menu_save:
    	    	chooseAndUseSaveSlot(true);
    	        return true;
    	    case R.id.menu_load:
    	    	chooseAndUseSaveSlot(false);
    	        return true;
    	        
    	    default:
    	        return super.onOptionsItemSelected(item);
    	    }
    }
    
    // ----------------------- UI call backs --------------------------------
    // calculator name touch callback
    public void onCalculatorName(View view) {
    	calcNameTouchCounter = (calcNameTouchCounter + 1) % 33;
    	if (calcNameTouchCounter != 0) return; // act only on every 33th click - hidden feature !
    	
    	setMkModel(1 - mkModel);
    	if (emulator != null)
    		emulator.setMkModel(mkModel);
    }

    // calculator model menu option callback
    public void onChooseMkModel() {
	    ContextThemeWrapper cw = new ContextThemeWrapper( this, R.style.AlertDialogTheme );
		AlertDialog.Builder builder = new AlertDialog.Builder(cw);
	
		builder.setSingleChoiceItems(new String[] {getString(R.string.item_mk61), getString(R.string.item_mk54)}, mkModel, new DialogInterface.OnClickListener() {
		    public void onClick(DialogInterface dialog, int item) {
		    	setMkModel(item);
				dialog.cancel();
		    }
		});

		AlertDialog alert = builder.create();
		alert.show();		
	}

    // calculator indicator touch callback
    public void onIndicator(View view) {
        if (emulator != null) {
        	emulator.setSpeedMode(1 - emulator.getSpeedMode());
        	speedMode = emulator.getSpeedMode();
        }
    	setIndicatorColor();
    }

    // calculator power switch callback
    public void onPower(View view) {
    	if (vibrate) vibrator.vibrate(VIBRATE_ON_OFF_SWITCH);
    	boolean isOn = ((CheckBox)view).isChecked();
    	switchOnCalculator(isOn);
    }
    
    // calculator mode switch callback
    public void onMode(View view) {
        angleMode = Integer.parseInt((String)view.getTag());
        if (emulator != null) {
        	emulator.setAngleMode(angleMode);
        	if (vibrate) vibrator.vibrate(VIBRATE_ANGLE_SWITCH);
        }
    }

    // calculator button press callback
    public void onButton(View view) {
    	if (emulator == null)
    		return;
    	
    	int keycode = Integer.parseInt((String)view.getTag());
    	emulator.keypad(keycode);
    	if (vibrate) vibrator.vibrate(VIBRATE_KEYPAD);
    }
    
    // on settings menu selected
    private void goSettingsScreen() {
    	Intent settingsScreen = new Intent(getApplicationContext(), SettingsActivity.class);
        startActivity(settingsScreen);
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

    // ----------------------- Dialogs --------------------------------
    private void chooseAndUseSaveSlot(final boolean save) {
		if (save && emulator == null) // disable saving when calculator is switched off
			return;
		
	    final CharSequence[] items = new CharSequence[SAVE_SLOTS_NUMBER];
	    for (int i=0; i < SAVE_SLOTS_NUMBER; i++) {
    		items[i] = getSlotDisplayName(i);
    	}
	
	    ContextThemeWrapper cw = new ContextThemeWrapper( this, R.style.AlertDialogTheme );
		AlertDialog.Builder builder = new AlertDialog.Builder(cw);
		builder.setTitle(getString(R.string.msg_choose_slot) + " " + (save ? getString(R.string.msg_save) : getString(R.string.msg_load)));
	
		builder.setSingleChoiceItems(items, selectedSaveSlot, new DialogInterface.OnClickListener() {
		    public void onClick(DialogInterface dialog, int item) {
		    	tempSaveSlot = item;
		    }
		});
	
		builder.setPositiveButton(save ? getString(R.string.label_save) : getString(R.string.label_load), 
				new DialogInterface.OnClickListener() {
		    @Override
		    public void onClick(DialogInterface dialog, int id) {
		    	if (save) {
		    		chooseNameAndSaveState();
		    	} else {
			    	if (loadState(tempSaveSlot))
				    	selectedSaveSlot = tempSaveSlot;
		    	}
		    }
		});
	
		builder.setNegativeButton(getString(R.string.label_cancel), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
					dialog.cancel();
			}
		});

		AlertDialog alert = builder.create();
		alert.show();		
	}
	
	private void aboutDialog() {
		AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
 
		// set title
		alertDialogBuilder.setTitle(getString(R.string.menu_about));
 
		String versionName = "";
		try {
			versionName = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
		} catch (NameNotFoundException e) {
		    e.printStackTrace();
		}
		    
		// set dialog message
		alertDialogBuilder.setMessage(MessageFormat.format(getString(R.string.msg_about), versionName));
		
		alertDialogBuilder.setNegativeButton(getString(R.string.label_close), new DialogInterface.OnClickListener() {
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
		textView.setTextSize(14);

	}
    
	// ----------------------- Save/Load emulator state --------------------------------
    private void chooseNameAndSaveState() {
    	String chosenName = emulator.getSaveStateName();
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.msg_choose_name));
        EditText input = new EditText(this);
        input.setId(EDIT_TEXT_ENTRY_ID);
        input.append(chosenName);
        input.setFilters(new InputFilter[] { new InputFilter.LengthFilter(SAVE_NAME_MAX_LEN) });
        builder.setView(input);
        builder.setPositiveButton(getString(R.string.label_save), new DialogInterface.OnClickListener() {
            //@Override
            public void onClick(DialogInterface dialog, int which) {
                EditText input = (EditText) ((Dialog)dialog).findViewById(EDIT_TEXT_ENTRY_ID);
                Editable value = input.getText();
                emulator.setSaveStateName(value.toString());
	    		saveState(tempSaveSlot); // stop and save
	    		loadState(tempSaveSlot); // keep going !
		    	selectedSaveSlot = tempSaveSlot;
            }
        });
        builder.show();

    }
    
    private String getSlotDisplayName(int i) {
    	String filename = getSlotFilename(i);
    	String i_str = "0" + (i+1);
    	i_str = i_str.substring(i_str.length()-2);
    	String slotName = "";
    	if (!getFileStreamPath(getSlotFilename(i)).exists()) {
    		slotName = getString(R.string.savestate_name_slot) + " " + i_str + " " + getString(R.string.savestate_name_empty);
    	} else {
			FileInputStream fileIn = null;
			ObjectInputStream in = null;
	
			try {
				fileIn = openFileInput(filename);
				in = new ObjectInputStream(fileIn);
				com.cax.pmk.emulator.Emulator.readStateNamesMode = true;
				EmulatorInterface emulatorObjForStateNameOnly = (com.cax.pmk.emulator.Emulator) in.readObject();
				if (emulatorObjForStateNameOnly.getSaveStateName() != null && emulatorObjForStateNameOnly.getSaveStateName().length() > 0)
					slotName = emulatorObjForStateNameOnly.getSaveStateName();
				in.close();
				fileIn.close();
		    } catch(Exception e) {
		    	e.printStackTrace();
			} finally {
				com.cax.pmk.emulator.Emulator.readStateNamesMode = false;
				try { if (in != null) in.close(); } catch(IOException e) {} 
				try { if (fileIn != null) fileIn.close(); } catch(IOException e) {}
			}
    	}
    	return "[" + i_str + "] " + ("".equals(slotName) ? getString(R.string.savestate_name_noname) : slotName);
    }
	
	String getSlotFilename(int slotNumber) {
    	String filename;
    	if (slotNumber < 0)
    		filename = PERSISTENCE_STATE_FILENAME;
    	else
    		filename = "slot" + slotNumber;
    	filename += PERSISTENCE_STATE_EXTENSION;
    	return filename;
    }
    
    boolean saveState(int slotNumber) {
    	if (emulator == null)
    		return false;
    	
    	emulator.stopEmulator();
    	
    	String filename = getSlotFilename(slotNumber);
    	
    	FileOutputStream fileOut = null;
    	ObjectOutputStream out = null;
		try {
			fileOut = openFileOutput(filename, Context.MODE_PRIVATE);
			out = new ObjectOutputStream(fileOut);
			out.writeObject(emulator);
			return true;
	    } catch(IOException i) {
			  return false;
		} finally {
	    	emulator = null;
			try { if (out != null)         out.close(); } catch(IOException i) {} 
			try { if (fileOut != null) fileOut.close(); } catch(IOException i) {}
		}
    }

    boolean loadState(int slotNumber) {
    	String filename = getSlotFilename(slotNumber);
    	
    	if (! getFileStreamPath(filename).exists())
    		return false;
    	
		FileInputStream fileIn = null;
		ObjectInputStream in = null;

		EmulatorInterface loadedEmulator = null;
		try {
			fileIn = openFileInput(filename);
			in = new ObjectInputStream(fileIn);
			loadedEmulator = (com.cax.pmk.emulator.Emulator) in.readObject();
			in.close();
			fileIn.close();
			
	    } catch(Exception i) {
			  return false;
		} finally {
			try { if (in != null)         in.close(); } catch(IOException i) {} 
			try { if (fileIn != null) fileIn.close(); } catch(IOException i) {}
		}

    	if (emulator != null) {
    		emulator.stopEmulator();
    	}
    	
    	emulator = loadedEmulator;
    	emulator.initTransient(this);

    	angleMode = emulator.getAngleMode();
    	speedMode = emulator.getSpeedMode();
    	setAngleModeRadios();

    	setMkModel(emulator.getMkModel());

    	setIndicatorColor();

    	powerOnOffCheckbox.setChecked(true);
    	
    	emulator.start();

		return true;
    }

    // ----------------------- Other --------------------------------
    void scaleButtonsTextSize() {
	    float smallerButtonTextSize = (float) (buttonTextSize * 0.95);
	    ((Button)findViewById(R.id.buttonReturn   )).setTextSize(TypedValue.COMPLEX_UNIT_PX, smallerButtonTextSize);
	    ((Button)findViewById(R.id.buttonStopStart)).setTextSize(TypedValue.COMPLEX_UNIT_PX, smallerButtonTextSize);

	    List<View> list = getAllChildrenBFS(findViewById(R.id.tableLayoutKeyboard));
	    for (int i=0; i < list.size(); i++) {
	    	if (list.get(i) instanceof Button) {
	    		((Button)list.get(i)).setTextSize(TypedValue.COMPLEX_UNIT_PX, buttonTextSize);
	    	} else if (list.get(i) instanceof TextView) {
	    		((TextView)list.get(i)).setTextSize(TypedValue.COMPLEX_UNIT_PX, labelTextSize);
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
		
    private void setAngleModeRadios() {
    	((RadioButton)findViewById(R.id.radioRadians)).setChecked(angleMode==0);
    	((RadioButton)findViewById(R.id.radioDegrees)).setChecked(angleMode==1);
    	((RadioButton)findViewById(R.id.radioGrads))  .setChecked(angleMode==2);
    }
    
    private void activateSettings() {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        vibrate = sharedPref.getBoolean(SettingsActivity.PREFERENCE_VIBRATE, true);
  		calculatorIndicator.setKeepScreenOn(sharedPref.getBoolean(SettingsActivity.PREFERENCE_SCREEN_ALWAYS_ON, true));
    }
        
    private void setIndicatorColor() {
    	String color = INDCATOR_OFF_COLOR;
    	if (emulator != null) {
    		color = emulator.getSpeedMode() == 0 ? INDCATOR_FAST_SPEED_COLOR : INDCATOR_SLOW_SPEED_COLOR;
    	}
    	((LinearLayout)findViewById(R.id.linearLayout_Indicator))
    			.setBackgroundColor(Color.parseColor(color));
    }
    
    private void switchOnCalculator(boolean enable) {
    	if (enable) {
    		if (powerOnOffCheckbox.isChecked()) {
	            emulator = new com.cax.pmk.emulator.Emulator();
	    		emulator.setAngleMode(angleMode);
	    		emulator.setSpeedMode(speedMode);
	    		emulator.setMkModel(mkModel);
	    		emulator.initTransient(this);
	        	setIndicatorColor();
	            emulator.start();
    		}
    	} else {
    		if (emulator != null) {
    			emulator.stopEmulator();
    			emulator = null;
    		}
            calculatorIndicator.setText(EMPTY_INDICATOR);
            powerOnOffCheckbox.setChecked(false);
            //erase persistence file
            File file = getFileStreamPath(getSlotFilename(-1));
            if (file.exists())
            	file.delete();
            setIndicatorColor();
    	}
    }
}
