package com.elastic.model;

import java.io.Serializable;
import java.util.Map;

public class Product implements Serializable {
	private String id;
	private String place;
	private String category;

	public String getProductType() {
		return productType;
	}

	public Product(String place, String category, String productType) {
		this.place = place;
		this.category = category;
		this.productType = productType;
	}

	public Product(String id, String place, String category, String productType, String type, Map<String, Object> attributes, String code) {

		this.id = id;
		this.place = place;
		this.category = category;
		this.productType = productType;
		this.type = type;
		this.attributes = attributes;
		this.code = code;
	}

	public void setProductType(String productType) {
		this.productType = productType;
	}

	private String productType;
	private String type;

	public String getPlace() {
		return place;
	}

	public void setPlace(String place) {
		this.place = place;
	}

	public Product(String id, String place, String category, String type, Map<String, Object> attributes, String code) {
		this.id = id;
		this.place = place;
		this.category = category;
		this.type = type;
		this.attributes = attributes;
		this.code = code;
	}

	public String getCategory() {

		return category;
	}

	public void setCategory(String category) {
		this.category = category;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public Map<String, Object> getAttributes() {
		return attributes;
	}

	public void setAttributes(Map<String, Object> attributes) {
		this.attributes = attributes;
	}

	Map<String, Object> attributes;
	private String code;

	public Product() {

	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

}
