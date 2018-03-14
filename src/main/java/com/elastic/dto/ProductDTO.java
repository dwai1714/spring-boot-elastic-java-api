package com.elastic.dto;

import com.elastic.model.Attributes_Order;
import com.elastic.model.Product;

import java.util.List;
import java.util.Map;

public class ProductDTO {
	private List<Product> products;
	private Map<String, List<String>> attributes;

	public List<GetOfferResponseDTO> getGetOfferResponseDTOS() {
		return getOfferResponseDTOS;
	}

	public void setGetOfferResponseDTOS(List<GetOfferResponseDTO> getOfferResponseDTOS) {
		this.getOfferResponseDTOS = getOfferResponseDTOS;
	}

	private List<GetOfferResponseDTO> getOfferResponseDTOS;
	//Map<String, Integer> order;


	public Attributes_Order getAttributes_orders() {
		return attributes_orders;
	}

	public void setAttributes_orders(Attributes_Order attributes_orders) {
		this.attributes_orders = attributes_orders;
	}

	Attributes_Order attributes_orders;


	public ProductDTO() {
	}

	public Map<String, List<String>> getAttributes() {
		return attributes;
	}

	public void setAttributes(Map<String, List<String>> attributes) {
		this.attributes = attributes;
	}

	public List<Product> getProducts() {
		return products;
	}

	public void setProducts(List<Product> products) {
		this.products = products;
	}


	/*public Map<String, Integer> getOrder() {
		return order;
	}

	public void setOrder(Map<String, Integer> order) {
		this.order = order;
	}
*/

}
