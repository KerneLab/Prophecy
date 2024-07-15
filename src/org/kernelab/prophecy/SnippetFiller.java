package org.kernelab.prophecy;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.kernelab.basis.JSON;
import org.kernelab.basis.JSON.JSAN;
import org.kernelab.basis.TextFiller;
import org.kernelab.basis.Tools;

public class SnippetFiller
{
	/**
	 * {@code <!--#DataId(,):xxxxx-->}
	 */
	public static final String	INFO_REGEX		= "(?s:<!--#(\\w+)(?:\\((.*?)\\))?:(.*?)-->)";

	public static final Pattern	INFO_PATTERN	= Pattern.compile(INFO_REGEX);

	/**
	 * {@code <!--@c1,c2(,):xxxxx-->}
	 */
	public static final String	INST_REGEX		= "(?s:<!--@([^:]*?)(?:\\((.*?)\\))?:(.*?)-->)";

	public static final Pattern	INST_PATTERN	= Pattern.compile(INST_REGEX);

	private StringBuilder		buffer;

	protected SnippetFiller fillInfoConfig(JSON config)
	{
		boolean found = false;

		do
		{
			Matcher matcher = INFO_PATTERN.matcher(this.getBuffer());

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

	protected SnippetFiller fillInstConfig(JSON config)
	{
		boolean found = false;

		do
		{
			Matcher matcher = INST_PATTERN.matcher(this.getBuffer());

			if (found = matcher.find())
			{
				String cols = matcher.group(1);
				String split = matcher.group(2);
				String repl = "";
				if (!cols.trim().isEmpty())
				{
					if (split == null)
					{
						for (String col : cols.split(","))
						{
							if (!config.containsKey(col))
							{
								repl = null;
								break;
							}
						}
					}
					else
					{
						boolean has = false;
						for (String col : cols.split(","))
						{
							if (config.containsKey(col))
							{
								has = true;
								break;
							}
						}
						if (!has)
						{
							repl = null;
						}
					}
				}
				if (repl == null)
				{
					repl = "";
				}
				else
				{
					if (split == null)
					{
						String expr = matcher.group(3);
						repl = fillWith(expr, config);
					}
					else
					{
						split = JSON.RestoreStringContent(split);
						String expr = matcher.group(3);
						List<String> list = new LinkedList<String>();
						for (String col : cols.split(","))
						{
							if (config.containsKey(col))
							{
								JSON conf = config.clone();
								conf.attr("_col", col);
								conf.attr("_val", config.attr(col));
								list.add(fillWith(expr, conf));
							}
						}
						repl = Tools.jointStrings(split, list);
					}
				}
				this.getBuffer().replace(matcher.start(), matcher.end(), repl);
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

	protected String fillWith(CharSequence snippet, JSON data)
	{
		return new TextFiller().reset(snippet).fillWith(data).toString();
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
			buffer.append(filler.reset(snippet).fillWith(json.attr("no", i).attr("cnt", data.length())).toString());
			if (split == null)
			{
				break;
			}
			i++;
		}

		return buffer.toString();
	}

	public SnippetFiller fillWith(ConfigLoader config, String inst)
	{
		if (inst != null)
		{
			this.fillInstConfig(config.getInstConfig().valJSON(inst, true));
		}
		return this.fillInfoConfig(config.getInfoConfig()) //
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
