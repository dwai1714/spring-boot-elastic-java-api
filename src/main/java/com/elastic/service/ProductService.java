package com.elastic.service;


import java.util.List;

import com.elastic.model.ProductDTO;
import com.elastic.model.SearchQueryDTO;

public interface ProductService {
	public ProductDTO getProductDTO(String type);
	public List getTypes();
	public ProductDTO getProductDTOFullText(String fullText);
	public ProductDTO getProductDTOMatchQuery(SearchQueryDTO searchQueryDTO);
	public void CreateData(String place, String type, String category, String excelFileName);
	public void CreateSameTypeDataWithMultipleExcel(String place, String type, String category, List<String> excelFileNames);

	}
