package com.elastic.dto;


import java.util.List;
import java.util.Map;

/**
 * This class will hold request atributes
 */
public class SearchQueryDTO {
    private String place;
    private String category;

    private double minOfferPrice;
    private String dummy;
    private double maxOfferPrice;

    public double getMinOfferPrice() {
        return minOfferPrice;
    }

    public void setMinOfferPrice(double minOfferPrice) {
        this.minOfferPrice = minOfferPrice;
    }




    public double getMaxOfferPrice() {
        return maxOfferPrice;
    }

    public void setMaxOfferPrice(double maxOfferPrice) {
        this.maxOfferPrice = maxOfferPrice;
    }


    public String getDummy() {
        return dummy;
    }

    public void setDummy(String dummy) {
        this.dummy = dummy;
    }

    public String getProductType() {
        return productType;
    }

    public void setProductType(String productType) {
        this.productType = productType;
    }

    public String productType;

    public SearchQueryDTO() {
    }

    public Map<String, List<String>> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, List<String>> attributes) {
        this.attributes = attributes;
    }

    private Map<String,List<String>> attributes;

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

   }
