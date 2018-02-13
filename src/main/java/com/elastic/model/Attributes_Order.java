package com.elastic.model;

import com.google.gson.internal.LinkedTreeMap;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

public class Attributes_Order implements Serializable {
    String Type;


    public Map<String, LinkedTreeMap<String, String>> getAttributes_metadata() {
        return attributes_metadata;
    }

    public void setAttributes_metadata(Map<String, LinkedTreeMap<String, String>> attributes_metadata) {
        this.attributes_metadata = attributes_metadata;
    }

    Map<String, LinkedTreeMap<String,String>>attributes_metadata;

    public Attributes_Order() {
    }


    public String getType() {

        return Type;
    }

    public void setType(String type) {
        Type = type;
    }

   }
