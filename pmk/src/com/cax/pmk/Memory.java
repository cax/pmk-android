package com.cax.pmk;

public class Memory
{
    public static final int MEM_BITLEN = 1008;
	short ix;

	public Memory()
	{
		for (int i = 0;i < MEM_BITLEN;i++)
		{
			memarray[i] = false;
		}
		ix=0;
	}

	public final boolean tick(boolean rm)
	{
		boolean ret = memarray[ix];
		memarray[ix] = rm;
		ix++;
		if (ix == MEM_BITLEN) ix=0;
		return ret;
	}

	private boolean[] memarray = new boolean[MEM_BITLEN];
}