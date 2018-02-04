package com.dc.elastic;

import org.elasticsearch.client.transport.TransportClient;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import com.elastic.service.ProductService;

@RunWith(SpringRunner.class)
@SpringBootTest
public class ElasticTests {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	@Autowired
	private TransportClient client;
	
	@Autowired
	private ProductService service;




	@Test
	public void getFullTextTest() {
		service.getProductDTOFullText("Fans LG");

	}
	
	
}
