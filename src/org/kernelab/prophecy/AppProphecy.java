package org.kernelab.prophecy;

import java.io.File;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;

import org.kernelab.basis.Tools;
import org.kernelab.basis.JSON.JSAN;

public class AppProphecy
{
	public static void main(String[] args)
	{
		try
		{
			JFileChooser fc = new JFileChooser(".");
			fc.setMultiSelectionEnabled(false);

			fc.setDialogTitle("Open Configure File ...");
			fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
			fc.setFileFilter(new FileFilter()
			{
				@Override
				public String getDescription()
				{
					return null;
				}

				@Override
				public boolean accept(File f)
				{
					return f.isDirectory() || f.getName().toLowerCase().endsWith(".xlsx");
				}
			});
			if (fc.showOpenDialog(null) == JFileChooser.APPROVE_OPTION)
			{
				File config = fc.getSelectedFile();

				File source = new File(config.getParentFile(), "source");
				if (!source.isDirectory())
				{
					throw new RuntimeException("Source directory " + Tools.getFilePath(source) + " not exist");
				}

				File target = new File(config.getParentFile(), "target");

				JSAN params = new JSAN().addAll(args == null ? new String[0] : args) //
						.addLast("-config", config.getAbsolutePath(), //
								"-source", source.getAbsolutePath(), //
								"-target", target.getAbsolutePath() //
						);

				Prophecy.main(params.toArray(new String[params.size()]));

				JOptionPane.showMessageDialog(null, "Output to " + Tools.getFilePath(target), "Done",
						JOptionPane.INFORMATION_MESSAGE);
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();

			StringBuilder buffer = new StringBuilder(e.getLocalizedMessage());
			for (StackTraceElement trace : e.getStackTrace())
			{
				buffer.append("\n ");
				buffer.append(trace.toString());
			}

			JOptionPane.showMessageDialog(null, buffer.toString(), "Error", JOptionPane.ERROR_MESSAGE);
		}
	}
}
