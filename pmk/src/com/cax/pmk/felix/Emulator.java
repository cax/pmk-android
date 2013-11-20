package com.cax.pmk.felix;

import java.io.*;
import com.cax.pmk.*;
import java.util.Arrays;

public class Emulator extends Thread implements EmulatorInterface
{
	private transient static MCU.ucmd_u[] ik1302_ucrom; 
	private transient static MCU.ucmd_u[] ik1303_ucrom; 
	private transient static MCU.ucmd_u[] ik1306_ucrom; 

	static { 
    	ik1302_ucrom = new MCU.ucmd_u[68];
    	ik1303_ucrom = new MCU.ucmd_u[68];
    	ik1306_ucrom = new MCU.ucmd_u[68];

    	for (int i=0; i < 68; i++) {
            ik1302_ucrom[i] = new MCU.ucmd_u(UCommands.ik1302_urom[i]);
    	    ik1303_ucrom[i] = new MCU.ucmd_u(UCommands.ik1303_urom[i]);
    	    ik1306_ucrom[i] = new MCU.ucmd_u(UCommands.ik1306_urom[i]);
    	}
	}
	
	public final void setAngleMode(int mode) { this.mode = mode; }
    public int getAngleMode() { return mode; }
	public final void setSpeedMode(int mode) { }
    public int getSpeedMode() { return 0; }
    
    public final void keypad(int keycode)
    {
        if (btnpressed != 0)
            return; //already processing one
        btnpressed = keycode = (keycode / 10) * 256 + keycode % 10;
    }

    public Emulator()
    {
            ik1302 = new MCU();
            ik1303 = new MCU();
            ik1306 = new MCU();
            
            ir2_1 = new Memory();
            ir2_2 = new Memory();

            loadRom();
            
            ik1302.init();
            ik1303.init();
            ik1306.init();

            chain = false;
            sync = false;
            k1 = false;
            k2 = false;
            btnpressed = 0;
    }

    public void initTransient(MainActivity mainActivity) {
    	this.mainActivity = mainActivity;
    	state = 1;
    	refSync   = new boolean[1];
    	refSeg    = new byte[1];
    	refDcycle = new int[1];
        display = new byte[12];
        oldDisplay = new byte[12];
    	System.arraycopy(emptyDisplay, 0, oldDisplay, 0, emptyDisplay.length );
    	displayString = new StringBuffer(24);
    	loadRom();
    }
    
    public final void stopEmulator()
    {
        this.state = 0;
        while (state == 0)
        	try { Thread.sleep(10); } catch (Exception e) {}
    }

    public void run()
    {
        byte seg=0;
        int cycle;
        boolean grd=false;

        while (true)
        {
            //2 ms = real time
        	//// try { sleep(1); } catch (Exception e) {}
            if(state != 1) // not running - exit now
            	break;
            	
            for(cycle = 0; cycle<168; cycle++)
            {
                k1 = false;
                k2 = false;
                if(ik1302.strobe())
                {
                    if(ik1302.dcount == 12) //d13
                    {
                        k2 = true;
                    }
                    if (btnpressed != 0)
                    {
                            if((ik1302.dcount+1)==(2+(btnpressed &0xff)))
                            {
                                switch(btnpressed>>>8)
                                {
                                    case 0x1:
                                        k1 = true;
                                        k2 = false;
                                        break;
                                    case 0x2:
                                        k2 = true;
                                        k1 = false;
                                        break;
                                    case 0x3:
                                        k1 = true;
                                        k2 = true;
                                        break;
                                }
                                btnpressed = 0; // found D
                            }
                     }
                }

				refSync[0]   = sync;
				refSeg[0]    = seg;
				refDcycle[0] = dcycle;

                ///ticks++;
                chain = ik1302.tick(chain, k1, k2, refDcycle, refSync, refSeg);

                sync   = refSync[0];
                seg    = refSeg[0];
                dcycle = refDcycle[0];

                if(ik1302.strobe())
                {
                    if((dcycle>1)&&(dcycle<14))
                    {
                    	display[dcycle-2] = seg;
                    }

                    if(sync)
                    {
                    	showOnDisplay();
                    }
                }
                else
                {
                    if(sync)
                    {
                    	System.arraycopy(emptyDisplay, 0, display, 0, emptyDisplay.length );
                    	showOnDisplay();
                    }
                }

                switch(mode)
                {
                    case 0: // rad
                    if(ik1303.dcount == 9) //D10
                        {
                            grd = false;
                        }
                        else
                        {
                            grd = true;
                        }
                        break;
                    case 1: // deg
						if(ik1303.dcount == 10) //D11
						{
							grd = false;
						}
						else
						{
							grd = true;
						}
						break;
                    case 2: // grd
						if(ik1303.dcount == 11) //D12
						{
							grd = false;
						}
						else
						{
							grd = true;
						}
						break;
                }

                chain = ik1303.tick(chain, grd,   false, null, null, null);
                ///ticks++;
                chain = ik1306.tick(chain, false, false, null, null, null);
                ///ticks++;

                chain = ir2_1.tick(chain);
                chain = ir2_2.tick(chain);

                ik1302.pretick(chain);
            }
        } // end-of-main-while-loop
        state = -1; // stopped
    }

    // -------------------------------- Private methods ---------------------------------
    private final void showOnDisplay()
    {
        if (Arrays.equals(oldDisplay, display))
        	return;

		int i;

		displayString.setLength(0);
		for(i = 0; i<9; i++)
		{
			displayString.append(segments[display[8-i]&0xf]);
			if((display[8-i]&0x80) != 0)
			{
				displayString.append('.');
			}
			else
			{
				displayString.append('/');
			}
		}
		for(i = 0;i<3;i++)
		{
			displayString.append(segments[display[11-i]&0xf]);
			if((display[11-i]&0x80) != 0)
			{
				displayString.append('.');
			}
			else
			{
				displayString.append('/');
			}
		}

		mainActivity.setDisplay(displayString.toString());
		
		byte[] swap = oldDisplay;
		oldDisplay = display;
		display = swap;
		
		/// debug printout
		/*
		System.out.println(displayString);
		if (startTime != 0) {
			long time = (System.nanoTime() - startTime);
			System.out.println("Ticks: " + ticks + " in ms: " + time/1000000 + ", ns/tick: " + (time/ticks));
		}
		startTime = System.nanoTime();
		ticks = 0;
		*/
    }

	/// long startTime=0;
    /// int ticks = 0;

    private void loadRom() {
    	ik1302.ucrom = ik1302_ucrom;
    	ik1303.ucrom = ik1303_ucrom;
    	ik1306.ucrom = ik1306_ucrom;
    	
        ik1302.asprom = Synchro.ik1302_srom;
        ik1303.asprom = Synchro.ik1303_srom;
        ik1306.asprom = Synchro.ik1306_srom;

        ik1302.cmdrom = MCommands.ik1302_mrom;
        ik1303.cmdrom = MCommands.ik1303_mrom;
        ik1306.cmdrom = MCommands.ik1306_mrom;
    }
    
    private MCU ik1302;
    private MCU ik1303;
    private MCU ik1306;
    private Memory ir2_1;
    private Memory ir2_2;
    private boolean chain;
    private boolean sync;
    private boolean k1;
    private boolean k2;
    private int dcycle;
    private int btnpressed;
    private int mode; // 0=rad, 1=deg, 2=grd
    
   	private transient MainActivity mainActivity;
	private transient boolean[] refSync;
	private	transient byte[]    refSeg;
	private transient int[]     refDcycle;
    private transient StringBuffer displayString;
    private byte[] display = new byte[12];
    private byte[] oldDisplay = new byte[12];
    private transient int state; // 1=running, 0=stop

    private static final char[] segments = {'0','1','2','3','4','5','6','7','8','9','-','L','C','D','E',' '};
   	private static final byte[] emptyDisplay = {0xF, 0xF, 0xF, 0xF, 0xF, 0xF, 0xF, 0xF, 0xF, 0xF, 0xF, 0xF};
	private static final long serialVersionUID = 1L;

	@Override
	public void readExternal(ObjectInput arg0) throws IOException,
			ClassNotFoundException {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void writeExternal(ObjectOutput arg0) throws IOException {
		// TODO Auto-generated method stub
		
	}
}
