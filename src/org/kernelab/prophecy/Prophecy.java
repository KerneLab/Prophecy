package org.kernelab.prophecy;

import java.io.File;
import java.io.IOException;

import org.kernelab.basis.Entrance;
import org.kernelab.basis.Tools;
import org.kernelab.basis.Variable;

public class Prophecy
{
	public static void main(String[] args) throws IOException
	{
		Entrance entr = new Entrance().handle(args);

		File config = new File(entr.parameter("config"));
		File sourceDir = new File(entr.parameter("source"));
		File targetDir = new File(entr.parameter("target"));

		if (Tools.getFilePath(targetDir).startsWith(Tools.getFilePath(sourceDir)))
		{
			throw new IllegalArgumentException("Target directory must not be subdirectory of the source.");
		}

		new Prophecy().init(config, entr).process(sourceDir, targetDir);
	}

	private ConfigLoader	config;

	private SnippetFiller	filler;

	protected ConfigLoader getConfig()
	{
		return config;
	}

	protected SnippetFiller getFiller()
	{
		return filler;
	}

	public Prophecy init(File file, Entrance entr) throws IOException
	{
		int hostConfigSheet = Variable.asInteger(entr.parameter("host.config.sheet"), 0);
		int hostConfigHeaderRow = Variable.asInteger(entr.parameter("host.config.head"), 1);
		int hostConfigFlagBegin = Variable.asInteger(entr.parameter("host.config.flag"), 4);
		int mapConfigSheet = Variable.asInteger(entr.parameter("map.config.sheet"), 1);
		int mapConfigBeginRow = Variable.asInteger(entr.parameter("map.config.first"), 0);

		this.config = new ConfigLoader() //
				.setConfigFile(file) //
				.reset() //
				.loadHostConfig(hostConfigSheet, hostConfigHeaderRow, hostConfigFlagBegin) //
				.loadMapConfig(mapConfigSheet, mapConfigBeginRow);

		this.filler = new SnippetFiller();

		return this;
	}

	public Prophecy process(File sourceDir, File targetDir) throws IOException
	{
		targetDir.mkdirs();

		for (File sourceFile : sourceDir.listFiles())
		{
			if (sourceFile.isDirectory())
			{
				process(sourceFile, new File(targetDir, sourceFile.getName()));
			}
			else if (sourceFile.isFile())
			{
				System.out.print(Tools.getFilePath(sourceFile));
				File targetFile = new File(targetDir, sourceFile.getName());
				this.getFiller().readTemplate(sourceFile).fillWith(this.getConfig()).writeTo(targetFile);
				System.out.print('\t');
				System.out.println(Tools.getFilePath(targetFile));
			}
		}

		return this;
	}
}
