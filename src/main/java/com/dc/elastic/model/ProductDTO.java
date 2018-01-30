package com.dc.elastic.model;

import java.util.List;
import java.util.Map;

public class ProductDTO {
	List<Product> products;
	Map<String, Map<String, Long>> attributes;
	Map<String, Integer> order;


	public ProductDTO() {
	}

	public List<Product> getProducts() {
		return products;
	}

	public void setProducts(List<Product> products) {
		this.products = products;
	}

	public Map<String, Map<String, Long>> getAttributes() {
		return attributes;
	}

	public void setAttributes(Map<String, Map<String, Long>> attributes) {
		this.attributes = attributes;
	}
	
	public Map<String, Integer> getOrder() {
		return order;
	}

	public void setOrder(Map<String, Integer> order) {
		this.order = order;
	}


}
