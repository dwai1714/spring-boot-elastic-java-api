package com.elastic.dto;

import java.io.Serializable;

public class Price implements Serializable{
    private double minPrice;

    public double getMinPrice() {
        return minPrice;
    }

    public void setMinPrice(double minPrice) {
        this.minPrice = minPrice;
    }

    public double getMaxPrice() {
        return maxPrice;
    }

    public void setMaxPrice(double maxPrice) {
        this.maxPrice = maxPrice;
    }

    public Price() {

    }

    public Price(double minPrice, double maxPrice) {

        this.minPrice = minPrice;
        this.maxPrice = maxPrice;
    }

    private double maxPrice;
}
