package com.cax.pmk;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import android.os.Bundle;
import android.app.Activity;
import android.content.Context;
import android.graphics.Typeface;
import android.view.View;
import android.widget.CheckBox;
import android.widget.RadioButton;
import android.widget.TextView;

public class MainActivity extends Activity {

    private static final String PERSISTENCE_STATE_FILENAME = "persist.pmk";
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
    	if (loadState(null)) {
        	((CheckBox)findViewById(R.id.checkBoxPowerOnOff)).setChecked(true);
        	mode = emulator.getMode();
        	((RadioButton)findViewById(R.id.radioRadians)).setChecked(mode==0);
        	((RadioButton)findViewById(R.id.radioDegrees)).setChecked(mode==1);
        	((RadioButton)findViewById(R.id.radioGrads))  .setChecked(mode==2);
    		emulator.initTransient(this);
            emulator.start();
    	} else {
    		switchOnCalculator(true);
    	}
    }

    @Override
    public void onStop() {
    	super.onStop();
    	if (emulator != null) {
    		emulator.stopEmulator();
        	saveState(null);
        	emulator = null;
    	}
    }
    
    /* disable Settings menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }
    */  

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

    // ----------------------- Save/Load emulator state --------------------------------
    boolean saveState(String filename) {
    	if (filename == null)
    		filename = PERSISTENCE_STATE_FILENAME;
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
			try { if (out != null) out.close(); } catch(IOException i) {} 
			try { if (fileOut != null) fileOut.close(); } catch(IOException i) {}
		}
    }

    boolean loadState(String filename) {
    	if (filename == null)
    		filename = PERSISTENCE_STATE_FILENAME;
		FileInputStream fileIn = null;
		ObjectInputStream in = null;

		try {
			fileIn = openFileInput(filename);
			in = new ObjectInputStream(fileIn);
			emulator = (Emulator) in.readObject();
			in.close();
			fileIn.close();
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
            File file = getFileStreamPath(PERSISTENCE_STATE_FILENAME);
            if (file.exists())
            	file.delete();
    	}
    }

}
