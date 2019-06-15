package org.kernelab.prophecy;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.kernelab.basis.JSON;
import org.kernelab.basis.JSON.JSAN;
import org.kernelab.basis.TextFiller;
import org.kernelab.basis.Tools;

public class SnippetFiller
{
	/**
	 * {@code <!--#DataId(,):<tag>xxxxx</tag>-->}
	 */
	public static final String	SNIPPET_REGEX	= "(?s:<!--#(\\w+)(?:\\((.*?)\\))?:(.*?)-->)";

	public static final Pattern	SNIPPET_PATTERN	= Pattern.compile(SNIPPET_REGEX);

	public static void main(String[] args) throws IOException
	{
		ConfigLoader cl = new ConfigLoader() //
				.setConfigFile(new File("./dat/CDMO_T1/集群配置清单.xlsx")) //
				.reset() //
				.loadHostConfig(0, 1, 4) //
				.loadMapConfig(1, 0);

		SnippetFiller sf = new SnippetFiller() //
				.readTemplate(new File("./dat/CDMO_T1/hadoop/HA/yarn-site.xml")) //
				.fillWith(cl);

		Tools.debug(sf.getBuffer());
	}

	private StringBuilder buffer;

	protected SnippetFiller fillHostConfig(JSON config)
	{
		boolean found = false;

		do
		{
			Matcher matcher = SNIPPET_PATTERN.matcher(this.getBuffer());

			if (found = matcher.find())
			{
				String dataId = matcher.group(1);
				String split = matcher.group(2);
				String snippet = matcher.group(3);

				this.getBuffer().replace(matcher.start(), matcher.end(),
						fillWith(snippet, split, config.valJSAN(dataId, true)));
			}
		}
		while (found);

		return this;
	}

	protected SnippetFiller fillMapConfig(JSON json)
	{
		this.setBuffer(new StringBuilder(new TextFiller().reset(this.getBuffer()).fillWith(json).toString()));
		return this;
	}

	protected String fillWith(CharSequence snippet, String split, JSAN data)
	{
		StringBuilder buffer = new StringBuilder();

		split = split != null ? JSON.RestoreStringContent(split) : split;

		boolean first = true;

		TextFiller filler = new TextFiller();

		int i = 1;
		for (JSON json : data.iterator(JSON.class))
		{
			if (first)
			{
				first = false;
			}
			else if (split != null)
			{
				buffer.append(split);
			}
			buffer.append(filler.reset(snippet).fillWith(json.attr("no", i)).toString());
			if (split == null)
			{
				break;
			}
			i++;
		}

		return buffer.toString();
	}

	public SnippetFiller fillWith(ConfigLoader config)
	{
		return this.fillHostConfig(config.getHostConfig()) //
				.fillMapConfig(config.getMapConfig());
	}

	public StringBuilder getBuffer()
	{
		return buffer;
	}

	public SnippetFiller readTemplate(File file) throws IOException
	{
		this.setBuffer(null);

		StringBuilder buffer = new StringBuilder(10000);

		FileReader reader = null;

		try
		{
			reader = new FileReader(file);

			char[] buf = new char[1000];
			int read = -1;
			while ((read = reader.read(buf)) != -1)
			{
				buffer.append(buf, 0, read);
			}
			this.setBuffer(buffer);
		}
		finally
		{
			if (reader != null)
			{
				try
				{
					reader.close();
				}
				catch (Exception e)
				{
				}
			}
		}

		return this;
	}

	protected SnippetFiller setBuffer(StringBuilder buffer)
	{
		this.buffer = buffer;
		return this;
	}

	public SnippetFiller writeTo(File file) throws IOException
	{
		FileWriter writer = null;

		try
		{
			writer = new FileWriter(file);
			writer.write(this.getBuffer().toString());
		}
		finally
		{
			if (writer != null)
			{
				try
				{
					writer.close();
				}
				catch (Exception e)
				{
				}
			}
		}
		return this;
	}
}
