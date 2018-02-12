package com.elastic;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

@SpringBootApplication
@EnableAutoConfiguration(exclude={DataSourceAutoConfiguration.class})
public class ElasticApplication  {

	public static void main(String[] args) {
		System.out.println("Inside Main Class");
		SpringApplication.run(ElasticApplication.class, args);
	}


	


}
