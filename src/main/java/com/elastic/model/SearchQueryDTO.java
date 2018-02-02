package com.elastic.model;


import java.util.List;
import java.util.Map;

public class SearchQueryDTO {
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

   }
