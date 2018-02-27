package com.elastic.dto;

import com.elastic.model.Product;

import java.io.Serializable;

public class ProductScoreDTO implements Serializable,Comparable<ProductScoreDTO> {
    private Product product;

    public ProductScoreDTO(Product product, Double score) {
        this.product = product;
        this.score = score;
    }

    private Double score;
    private Double priceMatchScore;

    public ProductScoreDTO(Product product, Double score, Double priceMatchScore, Double offerPrice) {
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


    public ProductScoreDTO() {
    }

    public ProductScoreDTO(Product product, Double score, Double priceMatchScore) {
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


    @Override
    public int compareTo(ProductScoreDTO productScoreDTO) {
        return this.score.compareTo(productScoreDTO.getScore());

    }
}
