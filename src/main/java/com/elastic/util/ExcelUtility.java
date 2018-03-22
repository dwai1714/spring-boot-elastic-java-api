package com.elastic.util;

import java.io.*;
import java.util.*;

import com.elastic.model.Product;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.gson.Gson;

public class ExcelUtility {

	private String fileName;
	
	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}


	public ExcelUtility() {
		super();
	}

	public <T> Set<List<T>> getCombinations(List<List<T>> lists) {
		Set<List<T>> combinations = new HashSet<List<T>>();
		Set<List<T>> newCombinations;

		int index = 0;

		// extract each of the integers in the first list
		// and add each to ints as a new list
		for (T i : lists.get(0)) {
			List<T> newList = new ArrayList<T>();
			newList.add(i);
			combinations.add(newList);
		}
		index++;
		while (index < lists.size()) {
			List<T> nextList = lists.get(index);
			newCombinations = new HashSet<List<T>>();
			for (List<T> first : combinations) {
				for (T second : nextList) {
					List<T> newList = new ArrayList<T>();
					newList.addAll(first);
					newList.add(second);
					newCombinations.add(newList);
				}
			}
			combinations = newCombinations;

			index++;
		}

		return combinations;
	}

	public List<String> getHeaders() throws IOException {

		FileInputStream file = new FileInputStream(new File(fileName));
		XSSFWorkbook workbook = new XSSFWorkbook(file);

		// Get first sheet from the workbook
		XSSFSheet sheet = workbook.getSheetAt(0);

		// Assuming "column headers" are in the first row
		XSSFRow header_row = sheet.getRow(0);
		int noOfColumns = sheet.getRow(0).getLastCellNum();
		List<String> headers = new ArrayList<String>();

		for (int i = 0; i < noOfColumns; i++) {
			XSSFCell header_cell = header_row.getCell(i);
			headers.add(header_cell.getStringCellValue());
			// Do something with string
		}
		workbook.close();
		file.close();
		return headers;
	}

	public <T> List<List<Object>> getColumnAsArray() throws Exception {
		{

			List<List<Object>> lists = new ArrayList();
			try {

				FileInputStream file = new FileInputStream(new File(fileName));

				// Get the workbook instance for XLS file
				XSSFWorkbook workbook = new XSSFWorkbook(file);

				// Get first sheet from the workbook
				XSSFSheet sheet = workbook.getSheetAt(0);

				int noOfColumns = sheet.getRow(0).getLastCellNum();
				System.out.println(noOfColumns);

				// Iterate through each rows from first sheet
				for (int i = 0; i < noOfColumns; i++) {
					List<Object> col = new ArrayList<Object>();

					Iterator<Row> rowIterator = sheet.iterator();

					while (rowIterator.hasNext()) {

						Row row = rowIterator.next();
						// System.out.print("Rownum is " + row.getRowNum());
						if (row.getRowNum() != 0) {
							Cell cell = row.getCell(i);

							if (cell != null) {
								// add the values of the cell to the Arraylist
								if (cell.getCellTypeEnum() == CellType.NUMERIC) {
									// System.out.print(cell.getNumericCellValue());
									String val = String.valueOf(cell.getNumericCellValue());
									col.add(new Double(cell.getNumericCellValue()));
								} else if (cell.getCellTypeEnum() == CellType.STRING) {
									// System.out.print(cell.getRichStringCellValue());
									col.add(cell.getStringCellValue());
								} else if (cell.getCellTypeEnum() == CellType._NONE) {
									// System.out.print(cell.getRichStringCellValue());
									col.add(cell.getStringCellValue());
								}
							}

						}

					}
					lists.add(col);

				}
				file.close();
				System.out.println("Size of List " + lists.size());

				return lists;

				// print the value of the cells which is stored in the the Arraylist

			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return null;
	}

	public void createJson() throws Exception {
		List<String> headers = getHeaders();
		Set<List<Object>> combs = getCombinations(getColumnAsArray());
		for (List<Object> list : combs) {
			Map<String, Object> excelMap = combineListsIntoOrderedMap(headers, list);
			Gson gson = new Gson();
			String json = gson.toJson(excelMap);
			System.out.println("map is  " + json.toString());
		}

	}

	public Map<String, Object> combineListsIntoOrderedMap(List<String> keys, List<Object> values) {
		if (keys.size() != values.size())
			throw new IllegalArgumentException("Cannot combine lists with dissimilar sizes");
		Map<String, Object> map = new LinkedHashMap<String, Object>();
		for (int i = 0; i < keys.size(); i++) {
			map.put(keys.get(i), values.get(i));
		}
		return map;
	}

	public Map<String, Object> combineListsIntoOrderedMapWithArray(List<String> keys, List<List<Object>> values) {
		if (keys.size() != values.size())
			throw new IllegalArgumentException("Cannot combine lists with dissimilar sizes");
		Map<String, Object> map = new LinkedHashMap<String, Object>();
		for (int i = 0; i < keys.size(); i++) {
			map.put(keys.get(i), values.get(i));
		}
		return map;
	}

	public Map<String,Map<String,Object>> createConfigData(List<String> headers) throws IOException {
		InputStream ExcelFileToRead = new FileInputStream(fileName);
		XSSFWorkbook wb = new XSSFWorkbook(ExcelFileToRead);
		Set<Integer> elements = new HashSet<Integer>();
		Map<Integer,Product> hMap = new HashMap<Integer,Product>();
		List<Product> alProducts = new ArrayList<Product>();


		//
		XSSFSheet sheet = wb.getSheetAt(0);
		XSSFRow row;
		XSSFCell cell;

		Iterator rows = sheet.rowIterator();
		int rowCount = 0;
		Map<String,Map<String,Object>> attributes_metadata = new HashMap<String,Map<String, Object>>();

		//loop through rows
		while (rows.hasNext()) {
			row = (XSSFRow) rows.next();
			Map<String,Object> subValues = new HashMap<String,Object>();
			if(rowCount==0){
				rowCount++;
				continue;
			}
			Iterator cells = row.cellIterator();
			int col =0;
			String  header = null;
			while (cells.hasNext()) {

				cell = (XSSFCell) cells.next();
				if(col==0){
					header = cell.getStringCellValue();
				}else{
					if(cell.getCellTypeEnum().equals(CellType.NUMERIC)) {
						subValues.put(headers.get(col), String.valueOf(Math.round(cell.getNumericCellValue())));
					}else{
						subValues.put(headers.get(col), cell.getStringCellValue());

					}
				}

				col++;
			}
			attributes_metadata.put(header,subValues);
			}
		return attributes_metadata;

	}
}
