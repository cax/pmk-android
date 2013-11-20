package com.cax.pmk;

import android.os.Bundle;
import android.app.Activity;
import android.graphics.Typeface;
import android.view.Menu;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;

public class MainActivity extends Activity {

	private Emulator emulator = null;
	public int mode = 0;
	private TextView calculatorDisplay = null;
		
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        calculatorDisplay = (TextView) findViewById(R.id.textView_Indicator);
        Typeface tf = Typeface.createFromAsset(this.getAssets(),
                "fonts/digital-7-mod.ttf");
        calculatorDisplay.setTypeface(tf);
    }

    @Override
    public void onStop() {
    	super.onStop();
        if (emulator != null)
        	emulator.enable(false);
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (emulator != null)
        	emulator.enable(false);
    }
    
    // show string on calculator display 
    public void setDisplay(final String text) {
    	runOnUiThread(new Runnable() {
    	   public void run() {
    		   if (calculatorDisplay != null)
    			   calculatorDisplay.setText(text);
    	   }
    	});
    }

    // calculator button press callback
    public void onButton(View view) {
    	int keycode = Integer.parseInt((String)view.getTag());
        keycode = (keycode / 10) * 256 + keycode % 10;
        System.out.println("Tag: " + view.getTag() + ", keycode=" + keycode);

    	if (emulator == null)
    		return;
    	
    	emulator.keypad(keycode);
    }

    // calculator power switch callback
    public void onPower(View view) {
    	if (((CheckBox)view).isChecked()) {
            emulator = new Emulator(this);
    		emulator.enable(true);
            emulator.start();
    	} else {
    		emulator.enable(false);
            calculatorDisplay.setText("");
    	}
    }
    
    // calculator mode switch callback
    public void onMode(View view) {
        mode = Integer.parseInt((String)view.getTag());
        if (emulator != null)
        	emulator.set_mode(mode);
        System.out.println("Set mode=" + mode);
    }
    
    /* disable Settings menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }
    */  
}
