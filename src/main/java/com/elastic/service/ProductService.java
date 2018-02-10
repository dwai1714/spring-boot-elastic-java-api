package com.elastic.service;


import java.io.IOException;
import java.util.List;

import com.elastic.model.ProductDTO;
import com.elastic.model.SearchQueryDTO;

public interface ProductService {
	public ProductDTO getProductDTO(String type);
	public List getTypes();
	public ProductDTO getProductDTOFullText(String fullText);
	public ProductDTO getProductDTOMatchQuery(SearchQueryDTO searchQueryDTO);
void readExcel() throws Exception;
}
