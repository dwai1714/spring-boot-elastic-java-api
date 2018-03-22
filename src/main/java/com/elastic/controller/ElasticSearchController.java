package com.elastic.controller;

import java.text.SimpleDateFormat;
import java.util.*;

import com.elastic.dto.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import com.elastic.service.ProductService;

@CrossOrigin
@RestController
@RequestMapping(value = "/products")
public class ElasticSearchController {
	@Autowired
	ProductService productService;

	@RequestMapping(value = "/partial/search/{type}", method = RequestMethod.GET)
	public ProductDTO get(@PathVariable String type) {
		return productService.getProductDTO(type);
	}

	@RequestMapping(value = "/productTypes", method = RequestMethod.GET)
	public List getTypes() {
		return productService.getTypes();
	}

	@RequestMapping(value = "/textSearch/{type}", method = RequestMethod.GET)
	public ProductDTO getFullTextResults(@PathVariable String type) {

	    return productService.getProductDTOFullText(type);
	}

	@RequestMapping(method = RequestMethod.POST, value = "/partial", produces = MediaType.APPLICATION_JSON_VALUE)
	public ProductDTO doPartialSearch(@RequestBody GetOfferSearchQueryDTO getOfferSearchQueryDTO) throws Exception {
		ProductDTO productDTO = productService.getProductDTOMatchQuery(getOfferSearchQueryDTO);
		return productDTO;
	}
	@RequestMapping(method = RequestMethod.GET, value = "/types",produces = MediaType.APPLICATION_JSON_VALUE)
	public List<String> getProductTypes(@RequestParam Map queryMap)  throws Exception {

		return  productService.getAllProductTypes(queryMap);

	}
	@RequestMapping(method = RequestMethod.POST, value = "/offers", produces = MediaType.APPLICATION_JSON_VALUE)
	public ConsumerOffer doOffersSearch(@RequestBody GetOfferSearchQueryDTO getOfferSearchQueryDTO) throws Exception {
		List <String> zipCodes = new ArrayList<String>();
		for(DeliveryLocationDTO deliveryLocationDTO: getOfferSearchQueryDTO.getDeliveryLocation()){
			zipCodes.add(deliveryLocationDTO.getZipcode());
		}
		setRetailerAttributes(getOfferSearchQueryDTO, zipCodes);
		ConsumerOffer productDTO = productService.offersSearch(getOfferSearchQueryDTO);
		setSearchQueryParams(getOfferSearchQueryDTO);
		productDTO.setGetOffersRequestDTO(getOfferSearchQueryDTO);

		return productDTO;
	}

	private void setSearchQueryParams(@RequestBody GetOfferSearchQueryDTO getOfferSearchQueryDTO) {
		getOfferSearchQueryDTO.setSubCategoryName(getOfferSearchQueryDTO.getProductType());
		getOfferSearchQueryDTO.setStartDate(new Date());
		Calendar c = Calendar.getInstance();
		c.setTime(new Date()); // Now use today date.
		c.add(Calendar.DATE, 3); // Adding 5 days
		Date toDateOutput = c.getTime();

		getOfferSearchQueryDTO.setEndDate(toDateOutput);
	}

	private void setRetailerAttributes(@RequestBody GetOfferSearchQueryDTO getOfferSearchQueryDTO, List<String> zipCodes) {
		Map<String,List<String> > attributes = getOfferSearchQueryDTO.getAttributes();
		attributes.put("Zipcode",zipCodes);
		List<String> deliveryMethod = new ArrayList<String>();
		deliveryMethod.add(getOfferSearchQueryDTO.getDeliveryMethod());
		attributes.put("DeliveryMethod",deliveryMethod);
		getOfferSearchQueryDTO.setAttributes(attributes);
	}

	@RequestMapping(method = RequestMethod.POST, value = "/upload", produces = MediaType.APPLICATION_JSON_VALUE)
	public void doPartialSearch(@RequestBody UploadDTO uploadDTO) throws Exception {
		String uploadType = uploadDTO.getUploadType();
		if("Config".equalsIgnoreCase(uploadType)){
			productService.CreateConfigurationData(uploadDTO.getPlaceName(),
					uploadDTO.getCategoryName(),uploadDTO.getSubCategoryName(),uploadDTO.getFileNames().get(0));

		}else{
			productService.CreateSameTypeDataWithMultipleExcel(uploadDTO.getPlaceName(),uploadDTO.getCategoryName(),
					uploadDTO.getSubCategoryName(),uploadDTO.getFileNames());
		}
		//ProductDTO productDTO = productService.getProductDTOMatchQuery(getOfferSearchQueryDTO);
			}



}