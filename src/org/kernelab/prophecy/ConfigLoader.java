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
		ConfigLoader cl = new ConfigLoader().setConfigFile(new File("./dat/CDMO_T1/集群配置清单.xlsx"));

		cl.loadHostConfig(0, 1, 4);

		Tools.debug(cl.getHostConfig().toString(0));
	}

	private File					configFile;

	private Workbook				configBook;

	private FormulaEvaluator		formulaEvaluator;

	private Map<Integer, String>	hostConfigHeader;

	private JSON					hostConfig;

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

	protected FormulaEvaluator getFormulaEvaluator()
	{
		return formulaEvaluator;
	}

	public JSON getHostConfig()
	{
		return hostConfig;
	}

	protected Map<Integer, String> getHostConfigHeader()
	{
		return hostConfigHeader;
	}

	public JSON getMapConfig()
	{
		return mapConfig;
	}

	protected void loadHeader(Sheet sheet, int headRow)
	{
		this.setHostConfigHeader(new LinkedHashMap<Integer, String>());

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

			this.getHostConfigHeader().put(index, mapHeader(column));

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
	public ConfigLoader loadHostConfig(int sheetIndex, int headRow, int flagFrom) throws IOException
	{
		Sheet sheet = this.getConfigBook().getSheetAt(sheetIndex);

		loadHeader(sheet, headRow);

		int rows = sheet.getLastRowNum();

		for (int r = headRow + 1; r <= rows; r++)
		{
			Row row = sheet.getRow(r);

			JSON info = this.makeInfoData(row, 0, flagFrom);

			for (int c = flagFrom; c < this.getHostConfigHeader().size(); c++)
			{
				if ("Y".equalsIgnoreCase(this.getCellContent(row.getCell(c))))
				{
					String header = this.getHostConfigHeader().get(c);

					JSAN jsan = this.getHostConfig().valJSAN(header);
					if (jsan == null)
					{
						jsan = new JSAN();
						this.getHostConfig().put(header, jsan);
					}

					jsan.add(info);
				}
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
			this.getMapConfig().attr(this.getCellContent(row.getCell(0)), this.getCellContent(row.getCell(1)));
		}
		return this;
	}

	protected JSON makeInfoData(Row row, int dataBeginColumn, int dataEndColumn)
	{
		JSON data = new JSON();

		for (int i = dataBeginColumn; i < dataEndColumn; i++)
		{
			data.attr(this.getHostConfigHeader().get(i), this.getCellContent(row.getCell(i)));
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
		else
		{
			return column;
		}
	}

	public ConfigLoader reset()
	{
		return this.resetHostConfig().resetMapConfig();
	}

	protected ConfigLoader resetHostConfig()
	{
		this.setHostConfig(new JSON());
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

	protected ConfigLoader setFormulaEvaluator(FormulaEvaluator formulaEvaluator)
	{
		this.formulaEvaluator = formulaEvaluator;
		return this;
	}

	protected ConfigLoader setHostConfig(JSON configData)
	{
		this.hostConfig = configData;
		return this;
	}

	protected ConfigLoader setHostConfigHeader(Map<Integer, String> header)
	{
		this.hostConfigHeader = header;
		return this;
	}

	protected ConfigLoader setMapConfig(JSON mapData)
	{
		this.mapConfig = mapData;
		return this;
	}
}
