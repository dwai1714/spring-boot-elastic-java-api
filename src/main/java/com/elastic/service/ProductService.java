package com.elastic.service;


import java.util.List;

import com.elastic.model.ProductDTO;
import com.elastic.model.SearchQueryDTO;

public interface ProductService {
	static final String INDEX_NAME = "my_kala";
	static final String TYPE_NAME = "products";
	static final String ATT_ORDER_INDEX_NAME = "my_kala_ord";
	static final String ORDER_TYPE_NAME = "attributes_order";

	
	public ProductDTO getProductDTO(String type);
	public List getTypes();
	public ProductDTO getProductDTOFullText(String fullText);
	public ProductDTO getProductDTOMatchQuery(SearchQueryDTO searchQueryDTO);
	public void CreateData(String place, String category, String type, String excelFileName);
	public void CreateSameTypeDataWithMultipleExcel(String place, String category, String type, List<String> excelFileNames);

	}
