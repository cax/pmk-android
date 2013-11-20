package com.cax.pmk;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import android.os.Bundle;
import android.os.Vibrator;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Color;
import android.graphics.LightingColorFilter;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;

public class MainActivity extends Activity {

    private static final String PERSISTENCE_STATE_FILENAME = "persist";
    private static final String PERSISTENCE_STATE_EXTENSION = ".pmk";
	private static final int    SAVE_SLOTS_NUMBER = 50;
	private static final boolean useFelixCode = false;
	private int selectedSaveSlot = 0;
    private int tempSaveSlot = 0;
	private EmulatorInterface emulator = null;
	private int angleMode = 0;
	private int speedMode = 0;
	private TextView calculatorDisplay = null;
	private Vibrator vibrator = null;
		
    // ----------------------- Activity life cycle handlers --------------------------------
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        vibrator = (Vibrator) this.getSystemService(VIBRATOR_SERVICE);
        
        findViewById(R.id.buttonF)    .getBackground().setColorFilter(new LightingColorFilter(0x00000000, 0x00F5E345)); // yellow
        findViewById(R.id.buttonK)    .getBackground().setColorFilter(new LightingColorFilter(0x00000000, 0x0071E3FF)); // blue
        findViewById(R.id.buttonClear).getBackground().setColorFilter(0xFFFF0000, PorterDuff.Mode.MULTIPLY);            // red
        
        int blackButtons[] = new int[] { 
        		R.id.buttonStepForward, R.id.buttonStepBack,    R.id.buttonReturn, R.id.buttonStopStart,
        		R.id.buttonRegisterToX, R.id.buttonXToRegister, R.id.buttonGoto,   R.id.buttonSubroutine
        };
        for (int button: blackButtons) {
        	findViewById(button).getBackground().setColorFilter(new LightingColorFilter(0,0));
        }

        calculatorDisplay = (TextView) findViewById(R.id.textView_Indicator);
        Typeface tf = Typeface.createFromAsset(this.getAssets(), "fonts/digital-7-mod.ttf");
        calculatorDisplay.setTypeface(tf);

        tf = Typeface.createFromAsset(this.getAssets(), "fonts/missing-symbols.ttf");
        ((TextView)findViewById(R.id.labelSquare))  .setTypeface(tf);
        ((TextView)findViewById(R.id.labelEpowerX)) .setTypeface(tf);
        ((TextView)findViewById(R.id.label10powerX)).setTypeface(tf);
        ((TextView)findViewById(R.id.labelXpowerY)) .setTypeface(tf);
        ((TextView)findViewById(R.id.labelDot))     .setTypeface(tf);
	}

    @Override
    public void onStart() {
    	super.onStart();
    	if (! loadState(-1)) {
    		switchOnCalculator(true);
    	}
    }

    @Override
    public void onStop() {
    	super.onStop();
       	saveState(-1);
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        //startActivity(new Intent(this, QuickPrefsActivity.class));
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	 switch (item.getItemId()) {
    	    case R.id.menu_about:
    	        aboutDialog();
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
    
    // ----------------------- UI callbacks --------------------------------
    // calculator indicator click callback
    public void onIndicator(View view) {
        if (emulator != null) {
        	emulator.setSpeedMode(1 - emulator.getSpeedMode());
        	speedMode = emulator.getSpeedMode();
        }
    	setIndicatorColor();
    }

    // calculator power switch callback
    public void onPower(View view) {
    	switchOnCalculator(((CheckBox)view).isChecked());
    	vibrator.vibrate(300);
    }
    
    // calculator mode switch callback
    public void onMode(View view) {
        angleMode = Integer.parseInt((String)view.getTag());
        if (emulator != null) {
        	emulator.setAngleMode(angleMode);
        	vibrator.vibrate(20);
        }
    }

    // calculator button press callback
    public void onButton(View view) {
    	if (emulator == null)
    		return;
    	
    	int keycode = Integer.parseInt((String)view.getTag());
    	emulator.keypad(keycode);
    	vibrator.vibrate(15);
    }
    
    // Show string on calculator display 
    // Not really a callback - called also from Emulator thread  
    public void setDisplay(final String text) {
    	runOnUiThread(new Runnable() {
    	   public void run() {
    		   if (calculatorDisplay != null)
    			   calculatorDisplay.setText(text);
    	   }
    	});
    }

    // ----------------------- Dialogs --------------------------------
    private void chooseAndUseSaveSlot(final boolean save) {
		if (save & emulator == null) // disable saving when calculator is switched off
			return;
		
	    final CharSequence[] items = new CharSequence[SAVE_SLOTS_NUMBER];
	    for (int i=0; i < SAVE_SLOTS_NUMBER; i++) {
    		items[i] = getSlotDisplayName(i);
    	}
	
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("Choose slot to " + (save ? "save" : "load"));
	
		builder.setSingleChoiceItems(items, selectedSaveSlot, new DialogInterface.OnClickListener() {
		    public void onClick(DialogInterface dialog, int item) {
		    	tempSaveSlot = item;
		    }
		});
	
		builder.setPositiveButton(save ? "Save" : "Load", new DialogInterface.OnClickListener() {
		    @Override
		    public void onClick(DialogInterface dialog, int id) {
		    	if (save)
		    		saveState(tempSaveSlot);

		    	if (loadState(tempSaveSlot))
			    	selectedSaveSlot = tempSaveSlot;
		    }
		});
	
		builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
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
			 + "Based on emu145 project\n"
			 + "by Felix Lazarev\n"
			 + "code.google.com/p/emu145\n"
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
    }

	// ----------------------- Save/Load emulator state --------------------------------
    private String getSlotDisplayName(int i) {
    	String filename = getSlotFilename(i);
    	String i_str = "0" + (i+1);
    	String slotName = "Slot " + i_str.substring(i_str.length()-2);
    	if (!getFileStreamPath(getSlotFilename(i)).exists()) {
    		slotName = slotName + " (empty)";
    	} else {
			FileInputStream fileIn = null;
			ObjectInputStream in = null;
	
			try {
				fileIn = openFileInput(filename);
				in = new ObjectInputStream(fileIn);
				com.cax.pmk.emulator.Emulator.readStateNamesMode = true;
				com.cax.pmk.emulator.Emulator emulatorObjForStateNameOnly = (com.cax.pmk.emulator.Emulator) in.readObject();
				if (emulatorObjForStateNameOnly.saveStateName != null && emulatorObjForStateNameOnly.saveStateName.length() > 0)
					slotName = emulatorObjForStateNameOnly.saveStateName;
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
    	return slotName;
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
			try { if (out != null) out.close(); } catch(IOException i) {} 
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
			loadedEmulator = useFelixCode 
            		? (com.cax.pmk.felix.Emulator)    in.readObject()
            		: (com.cax.pmk.emulator.Emulator) in.readObject();
			in.close();
			fileIn.close();
			
	    } catch(Exception i) {
			  return false;
		} finally {
			try { if (in != null) in.close(); } catch(IOException i) {} 
			try { if (fileIn != null) fileIn.close(); } catch(IOException i) {}
		}

    	if (emulator != null)
    		emulator.stopEmulator();
    		emulator = loadedEmulator;

        	((CheckBox)findViewById(R.id.checkBoxPowerOnOff)).setChecked(true);
        	angleMode = emulator.getAngleMode();
        	speedMode = emulator.getSpeedMode();
        	((RadioButton)findViewById(R.id.radioRadians)).setChecked(angleMode==0);
        	((RadioButton)findViewById(R.id.radioDegrees)).setChecked(angleMode==1);
        	((RadioButton)findViewById(R.id.radioGrads))  .setChecked(angleMode==2);
        	emulator.initTransient(this);
        	setIndicatorColor();
            emulator.start();

			return true;
    }

    // ----------------------- Other --------------------------------
    private void setIndicatorColor() {
    	String color = "#000000";
    	if (emulator != null) {
    		color = emulator.getSpeedMode() == 0 ? "#444444" : "#001500";
    	}
    	((LinearLayout)findViewById(R.id.linearLayout_Indicator))
    			.setBackgroundColor(Color.parseColor(color));
    }
    
    private void switchOnCalculator(boolean enable) {
    	if (enable) {
    		if (((CheckBox)findViewById(R.id.checkBoxPowerOnOff)).isChecked()) {
	            emulator = useFelixCode 
	            		? new com.cax.pmk.felix.Emulator()
	            		: new com.cax.pmk.emulator.Emulator();
	    		emulator.setAngleMode(angleMode);
	    		emulator.setSpeedMode(speedMode);
	    		emulator.initTransient(this);
	        	setIndicatorColor();
	            emulator.start();
    		}
    	} else {
    		if (emulator != null) {
    			emulator.stopEmulator();
    			emulator = null;
    		}
            calculatorDisplay.setText("");
            ((CheckBox)findViewById(R.id.checkBoxPowerOnOff)).setChecked(false);
            //erase persistence file
            File file = getFileStreamPath(getSlotFilename(-1));
            if (file.exists())
            	file.delete();
            setIndicatorColor();
    	}
    }
}
