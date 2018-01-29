package com.dc.elastic;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;

@SpringBootApplication
@EnableAutoConfiguration
public class ElasticApplication  {

	public static void main(String[] args) {
		System.out.println("Inside Main Class");
		SpringApplication.run(ElasticApplication.class, args);
	}


	


}
