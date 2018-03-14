package com.dc.elastic;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.elastic.dto.GetOfferSearchQueryDTO;
import org.elasticsearch.client.transport.TransportClient;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import com.elastic.ElasticApplication;
import com.elastic.service.ProductService;
import com.elastic.util.ExcelUtility;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = { ElasticApplication.class })
public class ElasticTests {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	@Autowired
	private TransportClient client;

	@Autowired
	private ProductService service;

	public void getExcelDataTest() throws Exception {
		ExcelUtility excelutil = new ExcelUtility();
		// System.out.println("Some Test");
		excelutil.createJson();

	}

	@Test
	public void getDataTest() throws Exception {
		System.out.println("Time Start is " + new Date());
		List<String> myList = new ArrayList<String>();
		myList.add("D:\\shravan\\files\\Bidet.xlsx");
		myList.add("D:\\shravan\\files\\Parts-urinal.xlsx");
		myList.add("D:\\shravan\\files\\Copy of Toilet.xlsx");
		myList.add("D:\\shravan\\files\\Urinals.xlsx");
		myList.add("D:\\shravan\\files\\parts-bidet.xlsx");
		myList.add("D:\\shravan\\files\\parts-toilet.xlsx");
		service.CreateSameTypeDataWithMultipleExcel("Home & Garden","Bathroom", "Toilets", myList);
		System.out.println("Time End with Toilets is " + new Date());
		/*List<String> tvFile = new ArrayList<String>();
		tvFile.add("D:\\shravan\\files\\TV.xlsx");
		service.CreateSameTypeDataWithMultipleExcel("Electronics","TV and Home Theater", "TV",tvFile );
		*/System.out.println("Time End with TV is " + new Date());

	}
	@Test
	public void checkRetailer() throws Exception {
		service.retailerSearch(new GetOfferSearchQueryDTO());

	}
}
