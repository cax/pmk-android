package com.cax.pmk;

import java.util.Arrays;

public class Emulator extends Thread
{
	long startTime=0;
    int ticks = 0;

    public final void on_sync(byte[] display)
    {
        if (Arrays.equals(old_display, display))
        	return;

		int i;

		disp.setLength(0);
		for(i = 0;i<9;i++)
		{
			disp.append(segments[display[8-i]&0xf]);
			if((display[8-i]&0x80) != 0)
			{
				disp.append('.');
			}
		}
		for(i = 0;i<3;i++)
		{
			disp.append(segments[display[11-i]&0xf]);
			if((display[11-i]&0x80) != 0)
			{
				disp.append('.');
			}
		}

		mainActivity.setDisplay(disp.toString());
		
		// debug printout
		System.out.println(disp);
		if (startTime != 0) {
			long time = (System.nanoTime() - startTime);
			System.out.println("Ticks: " + ticks + " in ms: " + time/1000000 + ", ns/tick: " + (time/ticks));
		}
		startTime = System.nanoTime();
		ticks = 0;
		
		byte[] swap = old_display;
		old_display = display;
		display = swap;
    }

    public final void enable(boolean en)
    {
        enabled = en;
    }

    public final void set_mode(int mod)
    {
        mode = mod;
    }

    public final void keypad(int key)
    {
        if (btnpressed != 0)
            return; //already processing one
        btnpressed = key;
    }

    MainActivity mainActivity;
    public Emulator(MainActivity mainActivity)
    {
    		this.mainActivity = mainActivity;
    		set_mode(mainActivity.mode);
    		
            int i;
            int j;
            enabled=false;

            ik1302 = new MCU(); // "IK1302"
            ik1303 = new MCU(); // "IK1303"
            ik1306 = new MCU(); // "IK1306"
            ir2_1 = new Memory();
            ir2_2 = new Memory();

            //load memory
            for(i = 0;i<68;i++)
            {
                ik1302.ucrom[i] = UCommands.ik1302_urom[i];
                ik1303.ucrom[i] = UCommands.ik1303_urom[i];
                ik1306.ucrom[i] = UCommands.ik1306_urom[i];
            }
            for(i = 0;i<128;i++)
            {
                for(j = 0;j<9;j++)
                {
                    ik1302.asprom[i][j] = Synchro.ik1302_srom[i][j];
                    ik1303.asprom[i][j] = Synchro.ik1303_srom[i][j];
                    ik1306.asprom[i][j] = Synchro.ik1306_srom[i][j];
                }
		    }

           for(i = 0;i<256;i++)
            {
                ik1302.cmdrom[i] = MCommands.ik1302_mrom[i];
                ik1303.cmdrom[i] = MCommands.ik1303_mrom[i];
                ik1306.cmdrom[i] = MCommands.ik1306_mrom[i];
            }

            ik1302.init();
            ik1303.init();
            ik1306.init();

            chain = false;
            sync = false;
            k1 = false;
            k2 = false;
            btnpressed = 0;
    }

    public void run()
    {
        byte seg=0;
        int cycle;
        boolean grd=false;

        //2 ms = real time
        while (true)
        {
        	//// try { sleep(1); } catch (Exception e) {}
            if(!enabled)
            	return;
            	
            for(cycle = 0;cycle<168;cycle++)
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

				tempRef_sync[0]   = sync;
				tempRef_seg[0]    = seg;
				tempRef_dcycle[0] = dcycle;

                ticks++;
                chain = ik1302.tick(chain, k1, k2, tempRef_dcycle, tempRef_sync, tempRef_seg);

                sync   = tempRef_sync[0];
                seg    = tempRef_seg[0];
                dcycle = tempRef_dcycle[0];

                if(ik1302.strobe())
                {
                    if((dcycle>1)&&(dcycle<14))
                    {
                    	display[dcycle-2] = seg;
                    }

                    if(sync)
                    {
                    	on_sync(display);
                    }
                }
                else
                {
                    if(sync)
                    {
                    	on_sync(empty_display);
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
                ticks++;
                chain = ik1306.tick(chain, false, false, null, null, null);
                ticks++;

                chain = ir2_1.tick(chain);
                chain = ir2_2.tick(chain);

                ik1302.pretick(chain);
            }
        }

    }

	private boolean[] tempRef_sync   = new boolean[1];
	private	byte[]    tempRef_seg    = new byte[1];
	private int[]     tempRef_dcycle = new int[1];

    private static final char[] segments = {'0','1','2','3','4','5','6','7','8','9','-','L','C','D','E',' '};
   	private static final byte[] empty_display = {0xF, 0xF, 0xF, 0xF, 0xF, 0xF, 0xF, 0xF, 0xF, 0xF, 0xF, 0xF};
    private byte[] display = new byte[12];
    private byte[] old_display = new byte[12];
    private StringBuffer disp = new StringBuffer(24);

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
    private int mode = 0; // 0=rad, 1=deg, 2=grd
    private boolean enabled;
}
