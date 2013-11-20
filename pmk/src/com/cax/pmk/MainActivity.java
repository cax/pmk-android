package com.cax.pmk;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import android.os.Bundle;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Typeface;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.RadioButton;
import android.widget.TextView;

public class MainActivity extends Activity {

    private static final String PERSISTENCE_STATE_FILENAME = "persist";
    private static final String PERSISTENCE_STATE_EXTENSION = ".pmk";
	private static final int    SAVE_SLOTS_NUMBER = 50;
    private int selectedSaveSlot = 0;
    private int tempSaveSlot = 0;
	private Emulator emulator = null;
	private int mode = 0;
	private TextView calculatorDisplay = null;
		
    // ----------------------- Activity life cycle handlers --------------------------------
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        calculatorDisplay = (TextView) findViewById(R.id.textView_Indicator);
        Typeface tf = Typeface.createFromAsset(this.getAssets(), "fonts/digital-7-mod.ttf");
        calculatorDisplay.setTypeface(tf);
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
	// calculator power switch callback
    public void onPower(View view) {
    	switchOnCalculator(((CheckBox)view).isChecked());
    }
    
    // calculator mode switch callback
    public void onMode(View view) {
        mode = Integer.parseInt((String)view.getTag());
        if (emulator != null)
        	emulator.setMode(mode);
    }

    // calculator button press callback
    public void onButton(View view) {
    	int keycode = Integer.parseInt((String)view.getTag());
        keycode = (keycode / 10) * 256 + keycode % 10;
        ///System.out.println("Tag: " + view.getTag() + ", keycode=" + keycode);

    	if (emulator == null)
    		return;
    	
    	emulator.keypad(keycode);
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
	    	String i_str = "0" + (i+1);
    		items[i] = "Slot " + i_str.substring(i_str.length()-2)
    				+ (getFileStreamPath(getSlotFilename(i)).exists() ? "" : " (empty)");
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
    	
    	if (emulator != null)
    		emulator.stopEmulator();
    		emulator = null;

		FileInputStream fileIn = null;
		ObjectInputStream in = null;

		try {
			fileIn = openFileInput(filename);
			in = new ObjectInputStream(fileIn);
			emulator = (Emulator) in.readObject();
			in.close();
			fileIn.close();
			
        	((CheckBox)findViewById(R.id.checkBoxPowerOnOff)).setChecked(true);
        	mode = emulator.getMode();
        	((RadioButton)findViewById(R.id.radioRadians)).setChecked(mode==0);
        	((RadioButton)findViewById(R.id.radioDegrees)).setChecked(mode==1);
        	((RadioButton)findViewById(R.id.radioGrads))  .setChecked(mode==2);
    		emulator.initTransient(this);
            emulator.start();

			return true;
	    } catch(Exception i) {
			  return false;
		} finally {
			try { if (in != null) in.close(); } catch(IOException i) {} 
			try { if (fileIn != null) fileIn.close(); } catch(IOException i) {}
		}
    }

    // ----------------------- Other --------------------------------
    private void switchOnCalculator(boolean enable) {
    	if (enable) {
    		if (((CheckBox)findViewById(R.id.checkBoxPowerOnOff)).isChecked()) {
	            emulator = new Emulator();
	    		emulator.setMode(mode);
	    		emulator.initTransient(this);
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
    	}
    }
}
