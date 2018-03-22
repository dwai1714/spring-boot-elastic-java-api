package com.elastic;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SpringBootApplication
@EnableAutoConfiguration(exclude={DataSourceAutoConfiguration.class})
public class ElasticApplication  {

	public static void main(String[] args) {
		SpringApplication.run(ElasticApplication.class, args);
		Map<String,Object> oMap = new HashMap<String,Object>();
		Map <String,ArrayList<String>> values =
				new HashMap<String, ArrayList<String>>();
		ArrayList<String> l1= new ArrayList<String>();
				l1.add("sss");
		values.put("val",l1);
		oMap.put("name","Shravan");
		oMap.put("values",values);
		System.out.println(oMap.toString());

		double d = 2.1;
		long  l = Math.round(d);
	}


	


}
