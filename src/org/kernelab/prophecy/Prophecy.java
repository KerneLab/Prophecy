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
		else
		{
			Tools.deleteDirectory(targetDir);

			if (!targetDir.isDirectory() && !targetDir.mkdirs())
			{
				throw new IOException("Target directory " + targetDir.getAbsolutePath()
						+ " was not directory but could not create one.");
			}
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
				.loadInfoConfig(hostConfigSheet, hostConfigHeaderRow, hostConfigFlagBegin) //
				.loadMapConfig(mapConfigSheet, mapConfigBeginRow);

		this.filler = new SnippetFiller();

		return this;
	}

	public Prophecy process(File sourceDir, File targetDir) throws IOException
	{
		for (File sourceFile : sourceDir.listFiles())
		{
			if (sourceFile.isDirectory())
			{
				if ("@".equals(sourceFile.getName()))
				{
					for (String inst : this.getConfig().getInstConfig().keySet())
					{
						process(sourceFile, new File(targetDir, inst), inst);
					}
				}
				else
				{
					File target = new File(targetDir, sourceFile.getName());
					target.mkdirs();
					process(sourceFile, target);
				}
			}
			else if (sourceFile.isFile())
			{
				process(sourceFile, new File(targetDir, sourceFile.getName()), null);
			}
		}
		return this;
	}

	public Prophecy process(File source, File target, String inst) throws IOException
	{
		if (source.isDirectory())
		{
			target.mkdirs();
			for (File file : source.listFiles())
			{
				process(file, new File(target, file.getName()), inst);
			}
		}
		else if (source.isFile())
		{
			System.out.print(Tools.getFilePath(source));
			this.getFiller().readTemplate(source).fillWith(this.getConfig(), inst).writeTo(target);
			System.out.print('\t');
			System.out.println(Tools.getFilePath(target));
		}
		return this;
	}
}
