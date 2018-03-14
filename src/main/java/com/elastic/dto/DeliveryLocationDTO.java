package com.elastic.dto;

import java.io.Serializable;

/**
 * This class hold delivery location details
 *
 */
public class DeliveryLocationDTO  implements Serializable {
    private String Zipcode;
    private String state;
    private String country;

    public String getZipcode() {
        return Zipcode;
    }

    public void setZipcode(String zipcode) {
        this.Zipcode = zipcode;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public DeliveryLocationDTO() {

    }

    public DeliveryLocationDTO(String zipcode, String state, String country) {

        this.Zipcode = zipcode;
        this.state = state;
        this.country = country;
    }
}
