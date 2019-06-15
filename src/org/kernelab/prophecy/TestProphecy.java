package org.kernelab.prophecy;

import java.io.IOException;

public class TestProphecy
{
	public static void main(String[] args) throws IOException
	{
		Prophecy.main(new String[] { //
				"-config", "./dat/CDMO_T1/集群配置清单.xlsx", //
				"-source", "./dat/CDMO_T1/source", //
				"-target", "./dat/CDMO_T1/target" //
		});
	}
}
