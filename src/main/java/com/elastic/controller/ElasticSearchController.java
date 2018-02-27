package com.elastic.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import com.elastic.dto.ProductDTO;
import com.elastic.dto.SearchQueryDTO;
import com.elastic.service.ProductService;

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

	@RequestMapping(value = "/textSearch/{type}", method = RequestMethod.GET)
	public ProductDTO getFullTextResults(@PathVariable String type) {
		return productService.getProductDTOFullText(type);
	}

	@RequestMapping(method = RequestMethod.POST, value = "/partial", produces = MediaType.APPLICATION_JSON_VALUE)
	public ProductDTO doPartialSearch(@RequestBody SearchQueryDTO searchQueryDTO) throws Exception {
		ProductDTO productDTO = productService.getProductDTOMatchQuery(searchQueryDTO);
		return productDTO;
	}
	@RequestMapping(method = RequestMethod.GET, value = "/types",produces = MediaType.APPLICATION_JSON_VALUE)
	public List<String> getProductTypes(@RequestParam Map queryMap)  throws Exception {

		return  productService.getAllProductTypes(queryMap);

	}


}