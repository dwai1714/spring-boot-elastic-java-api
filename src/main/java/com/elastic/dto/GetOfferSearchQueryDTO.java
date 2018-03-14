package com.elastic.dto;


import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * This class will hold request atributes
 */
public class GetOfferSearchQueryDTO {

    private String placeName;
    private String categoryName;
    private String subCategoryName;
    private String productType;
    private Price price;
    private List<DeliveryLocationDTO> deliveryLocation;
    private Date startDate;
    private Date endDate;

    public GetOfferSearchQueryDTO(String placeName, String categoryName, String subCategoryName, Date startDate, Date endDate, String deliveryMethod, Price price, List<DeliveryLocationDTO> deliveryLocation, String productType, Map<String, List<String>> attributes) {
        this.placeName = placeName;
        this.categoryName = categoryName;
        this.subCategoryName = subCategoryName;
        this.startDate = startDate;
        this.endDate = endDate;
        this.deliveryMethod = deliveryMethod;
        this.price = price;
        this.deliveryLocation = deliveryLocation;
        this.productType = productType;
        this.attributes = attributes;
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    public String getSubCategoryName() {
        return subCategoryName;
    }

    public void setSubCategoryName(String subCategoryName) {
        this.subCategoryName = subCategoryName;
    }

    public GetOfferSearchQueryDTO(String placeName, String categoryName, String deliveryMethod, Price price, List<DeliveryLocationDTO> deliveryLocation,
                                  String productType, Map<String, List<String>> attributes) {
        this.placeName = placeName;
        this.categoryName = categoryName;
        this.deliveryMethod = deliveryMethod;
        this.price = price;
        this.deliveryLocation = deliveryLocation;
        this.productType = productType;
        this.attributes = attributes;
    }

    public String getDeliveryMethod() {

        return deliveryMethod;
    }

    public void setDeliveryMethod(String deliveryMethod) {
        this.deliveryMethod = deliveryMethod;
    }

    private String deliveryMethod;


    public List<DeliveryLocationDTO> getDeliveryLocation() {
        return deliveryLocation;
    }

    public void setDeliveryLocation(List<DeliveryLocationDTO> deliveryLocation) {
        this.deliveryLocation = deliveryLocation;
    }

    public Price getPrice() {

        return price;
    }

    public void setPrice(Price price) {
        this.price = price;
    }


    public String getProductType() {
        return productType;
    }

    public void setProductType(String productType) {
        this.productType = productType;
    }


    public GetOfferSearchQueryDTO() {
    }

    public Map<String, List<String>> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, List<String>> attributes) {
        this.attributes = attributes;
    }

    private Map<String,List<String>> attributes;

    public String getPlaceName() {
        return placeName;
    }

    public void setPlaceName(String placeName) {
        this.placeName = placeName;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

   }
