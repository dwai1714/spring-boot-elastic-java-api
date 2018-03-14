package com.elastic.dto;

import com.elastic.model.Product;

import java.io.Serializable;

public class GetOfferResponseDTO implements Serializable{
    private Product product;
    private String retailerName;
    private String deliveryMethod;
    private Double difference;
    private Double score;
    private Double priceMatchScore;


    public GetOfferResponseDTO(Product product, String retailerName, String deliveryMethod, Double score, Double priceMatchScore, Double difference, Double offerPrice) {
        this.product = product;
        this.retailerName = retailerName;
        this.deliveryMethod = deliveryMethod;
        this.score = score;
        this.priceMatchScore = priceMatchScore;
        this.difference = difference;
        this.offerPrice = offerPrice;
    }

    public String getRetailerName() {
        return retailerName;
    }

    public void setRetailerName(String retailerName) {
        this.retailerName = retailerName;
    }

    public String getDeliveryMethod() {
        return deliveryMethod;
    }

    public void setDeliveryMethod(String deliveryMethod) {
        this.deliveryMethod = deliveryMethod;
    }

    public GetOfferResponseDTO(Product product, Double score) {
        this.product = product;
        this.score = score;
    }


    public GetOfferResponseDTO(Product product, Double score, Double priceMatchScore, Double difference, Double offerPrice) {
        this.product = product;
        this.score = score;
        this.priceMatchScore = priceMatchScore;
        this.difference = difference;
        this.offerPrice = offerPrice;
    }

    public Double getDifference() {

        return difference;
    }

    public void setDifference(Double difference) {
        this.difference = difference;
    }


    public GetOfferResponseDTO(Product product, Double score, Double priceMatchScore, Double offerPrice) {
        this.product = product;
        this.score = score;
        this.priceMatchScore = priceMatchScore;
        this.offerPrice = offerPrice;
    }

    public Double getOfferPrice() {

        return offerPrice;
    }

    public void setOfferPrice(Double offerPrice) {
        this.offerPrice = offerPrice;
    }

    private Double offerPrice;


    public Double getPriceMatchScore() {
        return priceMatchScore;
    }

    public void setPriceMatchScore(Double priceMatchScore) {
        this.priceMatchScore = priceMatchScore;
    }


    public GetOfferResponseDTO() {
    }

    public GetOfferResponseDTO(Product product, Double score, Double priceMatchScore) {
        this.product = product;
        this.score = score;
        this.priceMatchScore = priceMatchScore;
    }

    public Double getScore() {
        return score;
    }

    public void setScore(Double score) {
        this.score = score;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }


    }
