package com.dc.elastic.service;


import java.util.List;
import java.util.Map;

import com.dc.elastic.model.ProductDTO;

public interface ProductService {
	public ProductDTO getProductDTO(String type);
	public List getTypes();
	public ProductDTO getProductDTOFullText(String fullText);
	}
