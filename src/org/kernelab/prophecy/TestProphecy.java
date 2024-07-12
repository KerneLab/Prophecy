package org.kernelab.prophecy;

import java.io.IOException;

public class TestProphecy
{
	public static void main(String[] args) throws IOException
	{
		String base = "./dat/DEMO_ENV/";
		Prophecy.main(new String[] { //
				"-config", base + "conf.xlsx", //
				"-source", base + "source", //
				"-target", base + "target" //
		});
	}
}
