package com.elastic.util;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.gson.Gson;

public class CreateDataFromExcel {

	String fileName;
	private ExcelUtility excelutil;
	@Autowired
	private TransportClient client;


	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public ExcelUtility getExcelutil() {
		return excelutil;
	}

	public void setExcelutil(ExcelUtility excelutil) {
		this.excelutil = excelutil;
	}

	public CreateDataFromExcel(String fileName, ExcelUtility excelutil) {
		super();
		this.fileName = fileName;
		this.excelutil = excelutil;
	}

	public CreateDataFromExcel() {
		super();
	}

	public void createJson() throws Exception {
		excelutil.setFileName(this.fileName);
		List<String> headers = excelutil.getHeaders();
		Set<List<Object>> combs = excelutil.getCombinations(excelutil.getColumnAsArray());
		for (List<Object> list : combs) {
			Map<String, Object> excelMap = excelutil.combineListsIntoOrderedMap(headers, list);
			Gson gson = new Gson();
			String json = gson.toJson(excelMap);
			client.prepareIndex();
			IndexResponse response = client.prepareIndex("my_index", "dc_prod")
			        .setSource(json, XContentType.JSON)
			        .get();
			System.out.println("map is  " + response);
		}

	}

}
