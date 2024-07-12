package org.kernelab.prophecy;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellValue;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.kernelab.basis.JSON;
import org.kernelab.basis.JSON.JSAN;
import org.kernelab.basis.Tools;

public class ConfigLoader
{
	public static String CellContent(Cell cell, FormulaEvaluator ev)
	{
		String content = null;

		CellValue value = ev.evaluate(cell);

		if (value != null)
		{
			switch (value.getCellType())
			{
				case Cell.CELL_TYPE_STRING:
					content = value.getStringValue();
					break;

				case Cell.CELL_TYPE_NUMERIC:
					if (DateUtil.isCellDateFormatted(cell))
					{
						content = Tools.getDateTimeString(cell.getDateCellValue(), "yyyy-MM-dd");
					}
					else
					{
						content = String.valueOf(cell.getNumericCellValue()).replaceFirst("\\.0$", "");
					}
					break;

				case Cell.CELL_TYPE_BOOLEAN:
					content = String.valueOf(value.getBooleanValue());
					break;

				case Cell.CELL_TYPE_BLANK:
					break;

				case Cell.CELL_TYPE_ERROR:
					break;
			}
		}

		if (content == null)
		{
			content = "";
		}

		return content;
	}

	public static void main(String[] args) throws IOException
	{
		ConfigLoader cl = new ConfigLoader();

		cl.setConfigFile(new File("./dat/DEMO_ENV/conf.xlsx")) //
				.reset() //
				.loadInfoConfig(0, 1, 4);

		Tools.debug(cl.getInfoConfig().toString(0));
		Tools.debug(cl.getInstConfig().toString(0));
	}

	private File					configFile;

	private Workbook				configBook;

	private FormulaEvaluator		formulaEvaluator;

	private Map<Integer, String>	configHeader;

	private JSON					instConfig;

	private JSON					infoConfig;

	private JSON					mapConfig;

	public void close()
	{
		// TODO
	}

	protected String getCellContent(Cell cell)
	{
		return CellContent(cell, this.getFormulaEvaluator());
	}

	protected Workbook getConfigBook()
	{
		return configBook;
	}

	public File getConfigFile()
	{
		return configFile;
	}

	protected Map<Integer, String> getConfigHeader()
	{
		return configHeader;
	}

	protected FormulaEvaluator getFormulaEvaluator()
	{
		return formulaEvaluator;
	}

	public JSON getInfoConfig()
	{
		return infoConfig;
	}

	public JSON getInstConfig()
	{
		return instConfig;
	}

	public JSON getMapConfig()
	{
		return mapConfig;
	}

	protected void loadHeader(Sheet sheet, int headRow)
	{
		this.setConfigHeader(new LinkedHashMap<Integer, String>());

		Row head = sheet.getRow(headRow);
		Row prev = headRow > 0 ? sheet.getRow(headRow - 1) : null;

		int index = 0;

		for (Cell cell : head)
		{
			String column = this.getCellContent(cell);

			if (Tools.isNullOrEmpty(column) && prev != null)
			{
				column = this.getCellContent(prev.getCell(index));
			}

			this.getConfigHeader().put(index, mapHeader(column));

			index++;
		}
	}

	/**
	 * 加载节点配置
	 * 
	 * @param sheetIndex
	 *            节点配置的sheet
	 * @param headRow
	 *            表头行号
	 * @param flagFrom
	 *            打标开始列
	 * @return
	 * @throws IOException
	 */
	public ConfigLoader loadInfoConfig(int sheetIndex, int headRow, int flagFrom) throws IOException
	{
		Sheet sheet = this.getConfigBook().getSheetAt(sheetIndex);

		loadHeader(sheet, headRow);

		int instCol = -1;
		if ("实例".equals(getCellContent(sheet.getRow(headRow).getCell(flagFrom))))
		{
			instCol = flagFrom;
			flagFrom++;
		}

		int rows = sheet.getLastRowNum();

		for (int r = headRow + 1; r <= rows; r++)
		{
			Row row = sheet.getRow(r);
			if (row == null)
			{
				continue;
			}

			JSON info = this.makeInfoData(row, 0, flagFrom);
			JSON inst = instCol >= 0 ? info.clone() : null;

			for (int c = flagFrom; c < this.getConfigHeader().size(); c++)
			{
				String header = this.getConfigHeader().get(c);
				String text = this.getCellContent(row.getCell(c));

				if (text != null && !text.isEmpty())
				{
					info.attr(header, text);

					JSAN jsan = this.getInfoConfig().valJSAN(header);
					if (jsan == null)
					{
						jsan = new JSAN();
						this.getInfoConfig().put(header, jsan);
					}
					jsan.add(info);

					if (inst != null)
					{
						inst.attr(header, text);
					}
				}
			}

			if (inst != null)
			{
				this.getInstConfig().attr(inst.attrString("inst"), inst);
			}
		}

		return this;
	}

	/**
	 * 加载映射配置
	 * 
	 * @param sheetIndex
	 *            映射配置的sheet
	 * @param rowBegin
	 *            开始行号
	 * @return
	 */
	public ConfigLoader loadMapConfig(int sheetIndex, int rowBegin)
	{
		Sheet sheet = this.getConfigBook().getSheetAt(sheetIndex);

		int rows = sheet.getLastRowNum();

		for (int r = rows; r >= rowBegin; r--)
		{
			Row row = sheet.getRow(r);
			if (row == null)
			{
				continue;
			}
			this.getMapConfig().attr(this.getCellContent(row.getCell(0)), this.getCellContent(row.getCell(1)));
		}
		return this;
	}

	protected JSON makeInfoData(Row row, int dataBeginColumn, int dataEndColumn)
	{
		JSON data = new JSON();

		for (int i = dataBeginColumn; i < dataEndColumn; i++)
		{
			data.attr(this.getConfigHeader().get(i), this.getCellContent(row.getCell(i)));
		}

		return data;
	}

	protected String mapHeader(String column)
	{
		if ("序号".equals(column))
		{
			return "id";
		}
		else if ("地址".equals(column))
		{
			return "addr";
		}
		else if ("主机".equals(column))
		{
			return "host";
		}
		else if ("标签".equals(column))
		{
			return "tag";
		}
		else if ("实例".equals(column))
		{
			return "inst";
		}
		else
		{
			return column;
		}
	}

	public ConfigLoader reset()
	{
		return this.resetInfoConfig().resetInstConfig().resetMapConfig();
	}

	protected ConfigLoader resetInfoConfig()
	{
		this.setInfoConfig(new JSON());
		return this;
	}

	protected ConfigLoader resetInstConfig()
	{
		this.setInstConfig(new JSON());
		return this;
	}

	protected ConfigLoader resetMapConfig()
	{
		this.setMapConfig(new JSON());
		return this;
	}

	protected ConfigLoader setConfigBook(Workbook configBook)
	{
		this.configBook = configBook;
		if (configBook != null)
		{
			this.setFormulaEvaluator(configBook.getCreationHelper().createFormulaEvaluator());
		}
		return this;
	}

	public ConfigLoader setConfigFile(File file) throws IOException
	{
		this.setConfigBook(new XSSFWorkbook(new FileInputStream(file)));
		return this;
	}

	protected ConfigLoader setConfigHeader(Map<Integer, String> header)
	{
		this.configHeader = header;
		return this;
	}

	protected ConfigLoader setFormulaEvaluator(FormulaEvaluator formulaEvaluator)
	{
		this.formulaEvaluator = formulaEvaluator;
		return this;
	}

	protected ConfigLoader setInfoConfig(JSON configData)
	{
		this.infoConfig = configData;
		return this;
	}

	protected ConfigLoader setInstConfig(JSON instConfig)
	{
		this.instConfig = instConfig;
		return this;
	}

	protected ConfigLoader setMapConfig(JSON mapData)
	{
		this.mapConfig = mapData;
		return this;
	}
}
