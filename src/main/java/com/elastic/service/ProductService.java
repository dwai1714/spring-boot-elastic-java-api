package com.elastic.service;


import java.util.List;
import java.util.Map;

import com.elastic.dto.ConsumerOffer;
import com.elastic.dto.ProductDTO;
import com.elastic.dto.GetOfferSearchQueryDTO;
import com.elastic.dto.UploadDTO;
import org.springframework.web.multipart.MultipartFile;

public interface ProductService {
	static final String INDEX_NAME = "my_kala";
	static final String TYPE_NAME = "proPducts";

	static final String INDEX_NAME_TEST = "my_test";
	static final String TYPE_NAME_TEST = "test_products";

	static final String ATT_ORDER_INDEX_NAME = "my_kala_ord";
	static final String ORDER_TYPE_NAME = "attributes_order";

	
	public ProductDTO getProductDTO(String type);
	public List getTypes();
	public ProductDTO getProductDTOFullText(String fullText);
	public ProductDTO getProductDTOMatchQuery(GetOfferSearchQueryDTO getOfferSearchQueryDTO);
	public void CreateData(UploadDTO uploadDTO, MultipartFile multiPartFile);
	public void CreateSameTypeDataWithMultipleExcel(UploadDTO uploadDTO);

	List<String> getAllProductTypes(Map queryMap);

	ConsumerOffer offersSearch(GetOfferSearchQueryDTO getOfferSearchQueryDTO);
	ProductDTO retailerSearch(GetOfferSearchQueryDTO getOfferSearchQueryDTO);

	void CreateConfigurationData(UploadDTO uploadDTO);
}
