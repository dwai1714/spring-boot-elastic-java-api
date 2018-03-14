package com.elastic.model;

import java.io.Serializable;
import java.util.Map;

public class Product implements Serializable {
	private String id;
	private String place;
	private String category;
	private String type;
	private String retailerName;

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    private String productName;

	public String getRetailerName() {
		return retailerName;
	}

	public void setRetailerName(String retailerName) {
		this.retailerName = retailerName;
	}

	public String getRetailerId() {
		return retailerId;
	}

	public void setRetailerId(String retailerId) {
		this.retailerId = retailerId;
	}

	private String retailerId;

	public String getShippingProfileId() {
		return shippingProfileId;
	}

	public void setShippingProfileId(String shippingProfileId) {
		this.shippingProfileId = shippingProfileId;
	}

	private String shippingProfileId;

	public String getPlace() {
		return place;
	}

	public void setPlace(String place) {
		this.place = place;
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


	public Map<String, Object> getAttributes() {
		return attributes;
	}

	public void setAttributes(Map<String, Object> attributes) {
		this.attributes = attributes;
	}

	Map<String, Object> attributes;

	public Product() {

	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

}
