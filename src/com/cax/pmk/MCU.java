package com.cax.pmk;

public class MCU
{
	// ---------------------------------------------
		public final class ucmd_u
		{
			int raw;

				boolean a_r;
				boolean a_m;
				boolean a_st;
				boolean a_nr;
				boolean a_10nl;
				boolean a_s;
				boolean a_4;
				boolean b_s;

				boolean b_ns;
				boolean b_s1;
				boolean b_6;
				boolean b_1;
				boolean g_l;
				boolean g_nl;
				boolean g_nt;

				int r0;
				boolean r_1;
				boolean r_2;
				boolean m;
				boolean l;
				int s;

				int s1;
				int st;
				int pad;

		   public ucmd_u() {}

		   public void init(int u) {
			 raw = u;

			 a_r 	= (u & 1) > 0;
			 a_m 	= ((u >>  1) & 1) > 0;
			 a_st	= ((u >>  2) & 1) > 0;
			 a_nr	= ((u >>  3) & 1) > 0;
			 a_10nl	= ((u >>  4) & 1) > 0;
			 a_s	= ((u >>  5) & 1) > 0;
			 a_4	= ((u >>  6) & 1) > 0;
			 b_s	= ((u >>  7) & 1) > 0;

			 b_ns	= ((u >>  8) & 1) > 0;
			 b_s1	= ((u >>  9) & 1) > 0;
			 b_6	= ((u >> 10) & 1) > 0;
			 b_1	= ((u >> 11) & 1) > 0;
			 g_l	= ((u >> 12) & 1) > 0;
			 g_nl	= ((u >> 13) & 1) > 0;
			 g_nt	= ((u >> 14) & 1) > 0;

			 r0		= (u >> 15) & 7;
			 r_1	= ((u >> 18) & 1) > 0;
			 r_2	= ((u >> 19) & 1) > 0;
			 m		= ((u >> 20) & 1) > 0;
			 l		= ((u >> 21) & 1) > 0;
			 s		= (u >> 22) & 3;

			 s1		= (u >> 24) & 3;
			 st		= (u >> 26) & 3;
			 pad	= (u >> 28) & 15;
		 }
		}

	// ---------------------------------------------

    public MCU()
    {
        int i;
        for(i = 0;i<MCU_BITLEN;i++)
        {
            rm[i] = false;
            rr[i] = false;
            rs[i &3] = false;
            rs1[i &3] = false;
            rst[i] = false;
            rh[i &3] = false;

        }
    }

    public final void init()
    {
        icount = 0;
        dcount = 0;
        ecount = 0;
        ucount = 0;
        sigma = 0;
        carry = false;
        rl = false;
        rt = false;
        was_t_qrd = false;

        cptr = 0;

        command = cmdrom[cptr];
    }

    public final void pretick(boolean rin)
    {
        rm[rmIx==0?MCU_BITLEN-1:rmIx-1] = rin;
    }

	boolean strobe()
	{
		return (((command&0xfc0000)==0))?true:false;   //return value is D strobe idx for keyboard/display scan
	}

    public final boolean tick(boolean rin, boolean k1, boolean k2, int[] dcycle, boolean[] syncout, byte[] segment)
    {
        int ret;
        int a;
        int b;
        int g;
        int newr0=0;
        int newm0;
        boolean x;
        boolean y;
        boolean z;
        byte ucmd;
        boolean temp;

       	int mySeg;
		boolean tempBool;
		int tempInt;

        a = 0;
        b = 0;
        g = 0;

        //rm[MCU_BITLEN-1]=rin; //shift in the data

        if(icount<27)
        {
            ucmd = asprom[command &0x7f][jrom[icount]];
            asp = (byte) (command & 0x7f);
        }
        else
        {
            if((icount>=27)&&(icount<36))
            {
                ucmd = asprom[(command>>>8)&0x7f][jrom[icount]];
                asp = (byte) ((command>>>8)&0x7f);
            }
            else
            {
                // special case
                if(((command>>>16)&0xff)>=0x20)
                {
                    //r0 points to i=36
                    //we need to store ASP field to R1[D14:D13]
                    if(icount == 36)
                    {
                        rr[getRrIx(4)] = ((((command>>>16)&0xf)>>ucount)&1)>0?true:false;
                        rr[getRrIx(16)] = ((((command>>>20)&0xf)>>ucount)&1)>0?true:false;
                    }
                    ucmd = asprom[0x5f][jrom[icount]];
                    asp = 0x5f;
                }
                else
                {
                    ucmd = asprom[(command>>>16)&0x3f][jrom[icount]];
                    asp = (byte) ((command>>>16)&0x3f);
                }
            }
		} 
        ucmd&=0x3f; //fool's proof
        if(ucmd>0x3b)
        {
            ucmd = (byte)((ucmd-0x3c)*2);
            ucmd+=(!rl)?1:0;
            ucmd+=0x3c;
        }
        u_command.init(ucrom[ucmd]);

        switch(u_command.s1)
        {
            case 2:
                rh[0] = ((((k2?1:0)<<3|(k1?1:0))>>ucount)&1)>0;
                rs1[0]|=rh[0];
                break;
            case 3:
                rh[0] = ((((k2?1:0)<<3|(k1?1:0))>>ucount)&1)>0;
                rs1[0]|=rh[0];
                break;
        }

        if(k1|k2)
        {
           // rt=true;
            latchk1 = k1;
            latchk2 = k2;
        }
        if(u_command.g_nt)
        {
            was_t_qrd = true;
        }

        if(latchk1|latchk2)
        {
            rt = true;
        }
        else
        {
            rt = false;
        }

        //if((command&0xfc0000)==0)
        {
            if(u_command.g_nt | was_t_qrd)
            {
                rs1[0] = ((((latchk2?1:0)<<3|(latchk1?1:0))>>ucount)&1)>0?true:false;
            }
        }

        if(u_command.a_r)
        {
            a|=rr[rrIx]?1:0;
        }

        if(u_command.a_m)
        {
            a|=rm[rmIx]?1:0;
        }

        if(u_command.a_st)
        {
            a|=rst[rstIx]?1:0;
        }

        if(u_command.a_nr)
        {
            a|=(!rr[rrIx])?1:0;
        }

        if(u_command.a_10nl)
        {
            a|=((10>>ucount)&1) & ((!rl)?1:0);
        }

        if(u_command.a_s)
        {
            a|=rs[0]?1:0;
        }

        if(u_command.a_4)
        {
            a|=((4>>ucount)&1);
        }

        if(u_command.b_1)
        {
            b|=((1>>ucount)&1);
        }

        if(u_command.b_6)
        {
            b|=((6>>ucount)&1);
        }

        if(u_command.b_s)
        {
            b|=rs[0]?1:0;
        }

        if(u_command.b_s1)
        {
            b|=rs1[0]?1:0;
        }

        if(u_command.b_ns)
        {
            b|=(!rs[0])?1:0;
        }

        if(u_command.g_l)
        {
            g|=rl?1:0;
        }

        if(u_command.g_nl)
        {
            g|=(!rl)?1:0;
        }

        if(u_command.g_nt)
        {
             g|=(!rt)?1:0;
        }

        if(ucount!=0) //gamma input -- 1 bit data or carry only.
        {
            g = carry?1:0;
        }

        sigma = a+b+g;
        carry = ((sigma>>>1)&1)>0?true:false;
        sigma&=1;

        switch(u_command.r0)
        {
            case 0:
                newr0 = rr[rrIx]?1:0;
                break;
            case 1:
                newr0 = rr[getRrIx(12)]?1:0;
                break;
            case 2:
                newr0 = sigma;
                break;
            case 3:
                newr0 = rs[0]?1:0;
                break;
            case 4:
                newr0 = (rr[rrIx]?1:0)|(rs[0]?1:0)|sigma;
                break;
            case 5:
                newr0 = (rs[0]?1:0)|sigma;
                break;
            case 6:
                newr0 = (rr[rrIx]?1:0)|(rs[0]?1:0);
                break;
            case 7:
                newr0 = rr[rrIx]?1:0|sigma;
                break;
        }
        if(u_command.r_1)
        {
            if(icount<36)
            {
                if((command &0xff000000)==0)
                {
                    rr[getRrIx(MCU_BITLEN-4)] = sigma!=0;
                }
            }
            else
            {
                rr[getRrIx(MCU_BITLEN-4)] = sigma!=0;
            }
        }
        if(u_command.r_2)
        {
            if(icount<36)
            {
                if((command &0xff000000)==0)
                {
                    rr[getRrIx(MCU_BITLEN-8)] = sigma!=0;
                }
            }
            else
            {
                rr[getRrIx(MCU_BITLEN-8)] = sigma!=0;
            }

        }
        if(u_command.l)
        {
            if(ucount == 3)
            {
                rl = carry;
            }
        }

        if(u_command.m)
        {
            newm0 = rs[0]?1:0;
        }
        else
        {
            newm0 = rm[rmIx]?1:0;
        }

        switch(u_command.s)
        {
            case 0:
                temp = rs[0];
                rs[0] = rs[1];
                rs[1] = rs[2];
                rs[2] = rs[3];
                rs[3] = temp;
                break;
            case 1:
                rs[0] = rs[1];
                rs[1] = rs[2];
                rs[2] = rs[3];
                rs[3] = rs1[0];
                break;
            case 2:
                rs[0] = rs[1];
                rs[1] = rs[2];
                rs[2] = rs[3];
                rs[3] = sigma!=0;
                break;
            case 3:
                temp = rs1[0];
                rs1[0] = rs1[1];
                rs1[1] = rs1[2];
                rs1[2] = rs1[3];
                rs1[3] = (sigma|(temp?1:0))!=0;
                break;
        }

        switch(u_command.s1)
        {
            case 0:
                temp = rs1[0];
                rs1[0] = rs1[1];
                rs1[1] = rs1[2];
                rs1[2] = rs1[3];
                rs1[3] = temp;
                break;
            case 1:
                rs1[0] = rs1[1];
                rs1[1] = rs1[2];
                rs1[2] = rs1[3];
                rs1[3] = sigma!=0;
                break;

            case 2:
                temp = rs1[0];
                rs1[0] = rs1[1];
                rs1[1] = rs1[2];
                rs1[2] = rs1[3];
                rs1[3] = temp;
                break;

            case 3:
                temp = rs1[0];
                rs1[0] = rs1[1];
                rs1[1] = rs1[2];
                rs1[2] = rs1[3];
                rs1[3] = temp|(sigma!=0);
                break;
        }

		rstIx4 = (rstIx + 4) % MCU_BITLEN;
		rstIx8 = (rstIx + 8) % MCU_BITLEN;

        switch(u_command.st)
        {
            case 1:
                rst[rstIx8] = rst[rstIx4];
                rst[rstIx4] = rst[rstIx];
                rst[rstIx] = sigma!=0;
                break;
            case 2:
                temp = rst[rstIx];
                rst[rstIx] = rst[rstIx4];
                rst[rstIx4] = rst[rstIx8];
                rst[rstIx8] = temp;
                break;
            case 3:
                x = rst[rstIx];
                y = rst[rstIx4];
                z = rst[rstIx8];
                rst[rstIx ] = (sigma!=0)|y;
                rst[rstIx4] = x|z;
                rst[rstIx8] = x|y;
                break;
        }

        ret = newm0;

        rm[rmIx] = rin;
        rmIx++;
        if (rmIx == MCU_BITLEN) rmIx = 0;

        if((icount<36)&&(command & 0xff000000)!=0) //mod flag -- do not modify R!!!
        {
            newr0 = rr[rrIx]?1:0;
        }

        rr[rrIx] = newr0!=0;
        rrIx = getRrIx(1);

        temp = rh[0];
        rh[0] = rh[1];
        rh[1] = rh[2];
        rh[2] = rh[3];
        rh[3] = temp;

        if((dcount<13)&&(ecount == 0))
        {
            dispout = (newr0!=0?8:0)+(dispout>>>1);
        }

        rstIx++;
        if (rstIx == MCU_BITLEN) rstIx = 0;

        ucount++;
        if(ucount>=4)
        {
            ucount = 0;
            icount++;
            ecount++;
        }
        if(icount>=42)
        {
            icount = 0;

            cptr = (rr[getRrIx(156)]?1:0)|(rr[getRrIx(157)]?2:0)|(rr[getRrIx(158)]?4:0)|(rr[getRrIx(159)]?8:0);
            cptr = cptr<<4|(rr[getRrIx(144)]?1:0)|(rr[getRrIx(145)]?2:0)|(rr[getRrIx(146)]?4:0)|(rr[getRrIx(147)]?8:0);
            command = cmdrom[cptr];
            was_t_qrd = false;

            rt = false;
            latchk1 = false;
            latchk2 = false;
        }
        if(ecount>=3)
        {
            ecount = 0;
            dcount++;
         }
        if(dcount>=14)
        {
            dcount = 0;
        }

        //only needed for master - 1302
        if(dcycle != null && syncout != null)
        {
            dcycle[0] = (((command &0xfc0000)==0)&&(ecount == 0)&&(ucount == 0))?(dcount+1):0; //return value is D strobe idx for keyboard/display scan
            syncout[0] = ((dcount == 13)&&(ecount == 2)&&(ucount == 3))?true:false;
        }

        if (segment != null && dcycle != null)
        {
	        if (((command & 0xfc0000)==0)&&(ecount == 0)&&(ucount == 0))
            	mySeg = dispout;
            else
            	mySeg = 0;

            tempBool = (command &0xfc0000)==0 ? rl : false;
            tempInt = mySeg | (tempBool ? 0x80: 0);
            segment[0] = (byte) tempInt;
        }

        return (ret &1)>0?true:false;
    }
	// parameters:  rin - input register;
	//              k1 -- input pin K1 or H!!!
	//              k2 -- input pin K2
	//              *dcycle  -- current D cycle, output
	//              *syncout -- synchronization output for slaves
	//              *segment -- bit to display
	//              *point   -- decimal point
	//return value = output register

	public static final byte[] jrom = {0,1,2,3,4,5,3,4,5,3,4,5,3,4,5,3,4,5,3,4,5,3,4,5,6,7,8, 0,1,2,3,4,5,6,7,8, 0,1,2,3,4,5};
    public static final int MCU_BITLEN = 168;

    public int[] ucrom = new int[68]; //ucommand rom
    public byte[][] asprom = new byte[128][9]; //synchro sequence rom
    public int[] cmdrom = new int[256]; //macrocommand rom   -- format (asp0)|(asp1<<8)|(asp2<<16)|(modflag<<24)

	private short rmIx = 0;
    public boolean[] rm = new boolean[MCU_BITLEN];
    public boolean[] rr = new boolean[MCU_BITLEN];
    private int rrIx = 0;
    final public int getRrIx(int ix) { return (rrIx + ix) < MCU_BITLEN ? rrIx + ix : rrIx + ix - MCU_BITLEN; }
	private int rstIx=0, rstIx4, rstIx8;
    public boolean[] rst = new boolean[MCU_BITLEN];
    public boolean[] rs  = new boolean[4];
    public boolean[] rs1 = new boolean[4];
    public boolean[] rh  = new boolean[4];
    public int dispout;
    public boolean rl;
    public boolean rt;
    public boolean latchk1;
    public boolean latchk2;
    public int sigma; //so we can calculate normally
    public boolean carry;
    public ucmd_u u_command = new ucmd_u();
    public int command;
    public byte asp;
    public boolean was_t_qrd;
    public int icount;
    public int dcount;
    public int ecount;
    public int ucount;
    public int cptr;
}
