package com.elastic.model;

import java.util.List;
import java.util.Map;

public class ProductDTO {
	List<Product> products;
	Map<String, List<String>> attributes;
	Map<String, Integer> order;


	public ProductDTO() {
	}

	public Map<String, List<String>> getAttributes() {
		return attributes;
	}

	public void setAttributes(Map<String, List<String>> attributes) {
		this.attributes = attributes;
	}

	public List<Product> getProducts() {
		return products;
	}

	public void setProducts(List<Product> products) {
		this.products = products;
	}


	public Map<String, Integer> getOrder() {
		return order;
	}

	public void setOrder(Map<String, Integer> order) {
		this.order = order;
	}


}
