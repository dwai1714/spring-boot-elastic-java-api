package com.dc.elastic.controller;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.dc.elastic.model.ProductDTO;
import com.dc.elastic.service.ProductService;

@CrossOrigin
@RestController
@RequestMapping(value = "/products")
public class ElasticSearchController {
	@Autowired
	ProductService productService;

	@RequestMapping(value = "/partial/search/{type}", method = RequestMethod.GET)
	public ProductDTO get(@PathVariable String type) {
		return productService.getProductDTO(type);
	}

	@RequestMapping(value = "/productTypes", method = RequestMethod.GET)
	public List getTypes() {
		return productService.getTypes();
	}
}