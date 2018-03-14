package com.elastic.service;

import java.util.*;
import java.util.Map.Entry;

import com.elastic.dto.ConsumerOffer;
import com.elastic.dto.GetOfferResponseDTO;
import com.elastic.model.Attributes_Order;
import com.elastic.dao.OfferQuery;
import com.elastic.util.OffersAlgorithm;
import com.elastic.util.QueryUtility;
import com.google.gson.GsonBuilder;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.action.admin.indices.mapping.get.GetFieldMappingsResponse;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.search.MultiSearchResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.index.query.*;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.elastic.model.Product;
import com.elastic.dto.ProductDTO;
import com.elastic.dto.GetOfferSearchQueryDTO;
import com.elastic.util.ExcelUtility;
import com.google.gson.Gson;

@Service
/**
 * This class is for product services implementation
 */
public class ProductServiceImpl implements ProductService {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	@Autowired
	private TransportClient client;
	@Autowired
	private OfferQuery offerQuery;
	@Autowired
			private OffersAlgorithm offersAlgorithm ;
	@Autowired
	QueryUtility queryUtility;
	List<HashMap<String,Object>> listOfProducts = new ArrayList<HashMap<String, Object>>();

	@Override
	public ProductDTO getProductDTO(String type) {
		ProductDTO pDTO = new ProductDTO();
		SearchRequestBuilder requestAttOrderBuilder = getAttributeSearchRequestBuilder(type);
		// check attributes order
		SearchResponse attResponse = requestAttOrderBuilder.get();
		SearchHit[] hits = attResponse.getHits().getHits();
		if (hits.length == 0) {
			return pDTO;
		}

		// getOrder list
		List<Entry<String, Integer>> orderList = createOrderList(hits[0]);
		// actual order of attributes
		Map<String, Integer> order = queryUtility.getOrder(hits[0], "order");
		logger.info("Preparing query");
		SearchRequestBuilder requestBuilder = createProductSearchRequestBuilder(type, orderList);

		// Get response
		logger.info("Executing query");
		SearchResponse response = requestBuilder.get();

		//
		Map<String, List<String>> facets = queryUtility.getFacets(response, orderList,hits[0]);
		createSearchResult(pDTO, order, response, facets, null);
		return pDTO;
	}

	/**
	 * This method will create the search result from the search response
	 *  @param pDTO
	 * @param order
	 * @param response
	 * @param facets
	 * @param otherValueList
	 */
	private void createSearchResult(ProductDTO pDTO, Map<String, Integer> order, SearchResponse response,
									Map<String, List<String>> facets, Map<String, String> otherValueList) {
		SearchHit[] hits;
		hits = response.getHits().getHits();
		List<Product> products = new ArrayList<Product>();
		Arrays.asList(hits).forEach(hit -> {
			Map<String, Object> sourceObject = hit.getSourceAsMap();
			Map<String, Object> attributes = (Map<String, Object>) sourceObject.get("attributes");

			Product product = new Product();
			product.setType(sourceObject.get("type").toString());
			product.setPlace(sourceObject.get("place").toString());
			product.setCategory(sourceObject.get("category").toString());
			product.setId(hit.getId());
			product.setAttributes(attributes);
            if(null!=sourceObject.get("retailerName")) {
                product.setRetailerName(sourceObject.get("retailerName").toString());
            }if(null!=sourceObject.get("productName")) {
                product.setProductName(sourceObject.get("productName").toString());
            }
            product.setCategory(sourceObject.get("category").toString());

            products.add(product);

		});
		logger.info("Query Done");
		pDTO.setProducts(products);
		pDTO.setAttributes(facets);

		addAdditionalValues(pDTO, facets, otherValueList);
	}

	/**
	 *
	 * @param pDTO
	 * @param facets
	 * @param otherValueList
	 */
	private void addAdditionalValues(ProductDTO pDTO, Map<String, List<String>> facets, Map<String, String> otherValueList) {
		Set<String> otherValueKeys = otherValueList.keySet();
		for(String otherValueKey:otherValueKeys){
			String[] temp = null;
			String otherValue = otherValueList.get(otherValueKey);
			if(otherValue.contains(",")){
				temp = otherValue.split(",");
			}else{
				temp = new String[]{ otherValue};
			}
			List<String> facetValues = facets.get(otherValueKey);
			if(null!=facetValues) {
				for (String tempVal : temp) {
					facetValues.add(tempVal);
				}
				facets.put(otherValueKey,facetValues);

			}
			}

		pDTO.setAttributes(facets);
	}


	/**
	 * This method will create product search request builder with aggregations for
	 * single critera.
	 * 
	 * @param type
	 * @param orderList
	 * @return
	 */
	private SearchRequestBuilder createProductSearchRequestBuilder(String type,
			List<Entry<String, Integer>> orderList) {
		SearchRequestBuilder requestBuilder = client.prepareSearch(INDEX_NAME).setTypes(TYPE_NAME);
		AggregationBuilder aggregation = getAggregationBuilder(orderList);

		NestedQueryBuilder nqb = QueryBuilders.nestedQuery("attributes",
				QueryBuilders.boolQuery().must(QueryBuilders.matchQuery("attributes.Brand", "Orient")), ScoreMode.Max);
		QueryBuilder qb = QueryBuilders.boolQuery().must(QueryBuilders.matchQuery("type", type));

		requestBuilder.setQuery(qb);
		// requestBuilder.setQuery(nqb);
		requestBuilder.addAggregation(aggregation);
        AggregationBuilder aggregationType =  AggregationBuilders.terms("type").field("type"+ ".keyword");
        requestBuilder.addAggregation(aggregationType);
        return requestBuilder;
	}

	/**
	 * This method will build the aggreEgation builder with all the attributes
	 * 
	 * @param orderList
	 * @return
	 */
	private AggregationBuilder getAggregationBuilder(List<Entry<String, Integer>> orderList) {
		AggregationBuilder aggregation = AggregationBuilders.nested("all_attributes", "attributes");

		for (Entry<String, Integer> entry : orderList) {

			String termString = entry.getKey();

			if (getDataType("attributes", entry.getKey()).equals("text"))
				aggregation.subAggregation(
						AggregationBuilders.terms(entry.getKey()).field("attributes." + entry.getKey() + ".keyword"));
			else
				aggregation.subAggregation(
						AggregationBuilders.terms(entry.getKey()).field("attributes." + entry.getKey()));
		}
		return aggregation;
	}

	/**
	 * This method will create the order list
	 * 
	 * @param hit
	 * @return
	 */
	private List<Entry<String, Integer>> createOrderList(SearchHit hit) {
		Map<String, Integer> order = getOrder(hit);

		Set<Entry<String, Integer>> set = order.entrySet();
		List<Entry<String, Integer>> list = new ArrayList<Entry<String, Integer>>(set);
		Collections.sort(list, new Comparator<Map.Entry<String, Integer>>() {
			public int compare(Map.Entry<String, Integer> o1, Map.Entry<String, Integer> o2) {
				return (o1.getValue()).compareTo(o2.getValue());// Ascending order
			}
		});
		return list;
	}

	/**
	 * This method will get the raw order which needs to be processed further
	 * 
	 * @param hit
	 * @return
	 */
	private Map<String, Integer> getOrder(SearchHit hit) {
		Map<String, Object> attSource = hit.getSourceAsMap();
		return (Map<String, Integer>) attSource.get("order");
	}


	/**
	 * Attributes search query builder
	 * 
	 * @param type
	 * @return
	 */
	private SearchRequestBuilder getAttributeSearchRequestBuilder(String type) {
		SearchRequestBuilder requestAttOrderBuilder = client.prepareSearch(ATT_ORDER_INDEX_NAME).setTypes(ORDER_TYPE_NAME);
		QueryBuilder attQB = QueryBuilders.boolQuery().must(QueryBuilders.matchQuery("type", type));
		requestAttOrderBuilder.setQuery(attQB);
		return requestAttOrderBuilder;
	}

	@Override
	public ProductDTO getProductDTOFullText(String fullText) {
		SearchRequestBuilder searchqueryBuilder = client.prepareSearch(INDEX_NAME).setTypes(TYPE_NAME);
		SearchRequestBuilder PlainBuilder = client.prepareSearch(INDEX_NAME).setTypes(TYPE_NAME)
				.setQuery((QueryBuilders.queryStringQuery(fullText)));

		ProductDTO pDTO = new ProductDTO();

		NestedQueryBuilder nestedQueryBuilder = QueryBuilders.nestedQuery("attributes",
				QueryBuilders.boolQuery().must(QueryBuilders.queryStringQuery(fullText)), ScoreMode.Avg);

		searchqueryBuilder.setQuery(nestedQueryBuilder);

		MultiSearchResponse searchResponse = client.prepareMultiSearch().add(PlainBuilder).add(searchqueryBuilder)
				.get();

		getMultiSearchResponse(pDTO, searchResponse);

		return pDTO;
	}

	/**
	 * This method will be used to get the multi search response for full text
	 * search.
	 * 
	 * @param pDTO
	 * @param searchResponse
	 */
	private void getMultiSearchResponse(ProductDTO pDTO, MultiSearchResponse searchResponse) {
		for (MultiSearchResponse.Item item : searchResponse.getResponses()) {
			SearchResponse response = item.getResponse();
			logger.info("Response is " + response);
			SearchHit[] hits = response.getHits().getHits();
			List<Product> products = new ArrayList<Product>();
			Arrays.asList(hits).forEach(hit -> {
				Map<String, Object> sourceObject = hit.getSourceAsMap();
				Map<String, Object> attributes = (Map<String, Object>) sourceObject.get("attributes");

				Product product = new Product();
				product.setType(sourceObject.get("type").toString());
				product.setPlace(sourceObject.get("place").toString());
				product.setCategory(sourceObject.get("category").toString());
				product.setId(hit.getId());
				product.setAttributes(attributes);

				products.add(product);

			});
			logger.info("Query Done");
			if (null != pDTO.getProducts()) {
				List<Product> productsOld = pDTO.getProducts();
				for (Product product : products) {
					productsOld.add(product);
				}
				pDTO.setProducts(productsOld);

			} else {
				pDTO.setProducts(products);

			}

		}
	}

	@Override
	public List getTypes() {
		SearchRequestBuilder requestBuilder = client.prepareSearch(INDEX_NAME).setTypes(TYPE_NAME);
		SearchResponse response = requestBuilder
				.addAggregation(AggregationBuilders.terms("by_types").field("type.keyword")).execute().actionGet();
		Terms terms = response.getAggregations().get("by_types");
		List types = new ArrayList();
		terms.getBuckets().forEach(bucket -> {
			String keyString = bucket.getKeyAsString();
			types.add(keyString);

		});

		return types;

	}

	public String getDataType(String nestedField, String field) {

		GetFieldMappingsResponse response = client.admin().indices().prepareGetFieldMappings().addIndices(INDEX_NAME)
				.addTypes(TYPE_NAME).setFields(nestedField + "." + field).get();

		Map<String, Object> fieldSource;
		try {
			fieldSource = response.fieldMappings(INDEX_NAME, TYPE_NAME, nestedField + "." + field).sourceAsMap();
		} catch (Exception e) {
			// Bad Code but will figure later
			return "text";
		}

		String typeIs = (String) ((LinkedHashMap) (fieldSource.get(field))).get("type");
		return typeIs;

	}

	@Override
	public ProductDTO getProductDTOMatchQuery(GetOfferSearchQueryDTO getOfferSearchQueryDTO) {


		ProductDTO pDTO = new ProductDTO();

		SearchRequestBuilder requestAttOrderBuilder = getAttributeSearchRequestBuilder(getOfferSearchQueryDTO.getProductType());
		SearchResponse attResponse = requestAttOrderBuilder.get();
		SearchHit[] hits = attResponse.getHits().getHits();
		Attributes_Order attributes_order = new Attributes_Order();
		for(SearchHit hit : hits){
			String source =hit.getSourceAsString();
			if (source != null) {
				Gson gson = new GsonBuilder().setDateFormat("yyyMMdd")
						.create();
				 attributes_order = gson.fromJson(source, Attributes_Order.class);
				}
			String sourceAsString = hit.getSourceAsString();

		}
		if (hits.length == 0) {
			logger.info("hits is zero");
			return pDTO;
		}
		// getOrder list
		List<Entry<String, Integer>> orderList = queryUtility.createOrderList(hits[0],"order");
		Map<String,String> otherValueList = queryUtility.getAttributeValues(hits[0],"additionalValues");
		// actual order of attributes
		Map<String, Integer> order = queryUtility.getOrder(hits[0], "order");
		Map<String, Integer> range = queryUtility.getOrder(hits[0], "range");
		Map<String, Integer> importanceMap = queryUtility.getOrder(hits[0], "importance");
		SearchRequestBuilder plainQBuilder = null;
			plainQBuilder = createQueries(getOfferSearchQueryDTO, orderList,range);
		//plainQBuilder.setSize(3000);

		logger.info("Preparing query");

		SearchResponse response = plainQBuilder.get();
		Map<String, List<String>> facets = queryUtility.getFacets(response, orderList,hits[0]);
		pDTO.setAttributes_orders(attributes_order);
		createSearchResult(pDTO, order, response, facets,otherValueList);
		//Get Offers Alogorithm
		return pDTO;
	}

	/**
	 * This method create queries for multiple options selected and product type
	 * 
	 * @param getOfferSearchQueryDTO
	 * @param orderList
	 * @param range
	 * @return
	 */
	private SearchRequestBuilder createQueries(GetOfferSearchQueryDTO getOfferSearchQueryDTO, List<Entry<String, Integer>> orderList, Map<String, Integer> range) {
		BoolQueryBuilder queryBuilder = QueryBuilders.boolQuery();
		RangeQueryBuilder rq = null;
		if (null != getOfferSearchQueryDTO.getAttributes()) {
			Set<String> keys = getOfferSearchQueryDTO.getAttributes().keySet();
			for (String key : keys) {
				List<String> attributeValuesList = getOfferSearchQueryDTO.getAttributes().get(key);

				if(null!=range && range.keySet().contains(key) && range.get(key)>0){
					for (String value : attributeValuesList) {
						String[] values = queryUtility.splitValue(value);
						 rq = QueryBuilders.rangeQuery("attributes."+key);
						if(queryUtility.checkNumeric(values[0])){
							rq.gte(values[0]);
						}
						if(queryUtility.checkNumeric((values[1]))){
							rq.lt(values[1]);
						}
					}
				}else{
					if (null != attributeValuesList && attributeValuesList.size() > 1) {
						for (String value : attributeValuesList) {
							queryBuilder.should(QueryBuilders.matchQuery("attributes." + key, value));
						}
					}else if(null != attributeValuesList && attributeValuesList.size() > 0){
						for (String value : attributeValuesList) {
							queryBuilder.must(QueryBuilders.matchQuery("attributes." + key, value));
						}//for

						} //else if
				}//else
			}
		}
		SearchRequestBuilder plainBuilder = client.prepareSearch(INDEX_NAME).setTypes(TYPE_NAME);
			plainBuilder.setQuery(QueryBuilders.boolQuery()
					.must(QueryBuilders.matchQuery("type", getOfferSearchQueryDTO.getProductType()))
					.mustNot(QueryBuilders.nestedQuery("attributes",QueryBuilders.existsQuery("attributes.Quantity"),ScoreMode.Max)).
							must(QueryBuilders.nestedQuery("attributes",queryBuilder,ScoreMode.Max)));
		AggregationBuilder aggregation = getAggregationBuilder(orderList);
		plainBuilder.addAggregation(aggregation);
		return plainBuilder;
	}


	@Override
	public void CreateData(String place, String category, String type, String excelFileName) {
		ExcelUtility excelutil = new ExcelUtility();
		excelutil.setFileName(excelFileName);
		List<String> headers;
		try {
			headers = excelutil.getHeaders();

			Set<List<Object>> combs = excelutil.getCombinations(excelutil.getColumnAsArray());
			for (List<Object> list : combs) {
				HashMap<String,Object> topMap = new HashMap<String,Object>();
				topMap.put("place", place);
				topMap.put("type", type);
				topMap.put("category", category);
				topMap.put("dummy","yes");
				Map<String, Object> excelMap = excelutil.combineListsIntoOrderedMap(headers, list);
				topMap.put("attributes",excelMap);
				listOfProducts.add(topMap);
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		//doCommit();
	}

	@Override
	public void CreateSameTypeDataWithMultipleExcel(String place, String category, String type,
			List<String> excelFileNames) {
		for (int i = 0; i < excelFileNames.size(); i++) {
			CreateData(place, category, type, excelFileNames.get(i));
		}
		doCommit();


	}

	private void doCommit() {
		BulkRequestBuilder brb = client.prepareBulk();
		int productCount = 1;
		for (HashMap<String, Object> product : listOfProducts) {
			if (productCount < 5000) {
				brb.add(client.prepareIndex(INDEX_NAME, TYPE_NAME).setSource(product));
				productCount++;
			} else {
				BulkResponse bulkResponse = brb.execute().actionGet();
				brb = null;
				brb = client.prepareBulk();
				productCount = 1;
			}
		}
			BulkResponse bulkResponse = brb.execute().actionGet();
	}

	@Override
	public List<String> getAllProductTypes(Map queryMap) {
		BoolQueryBuilder queryBuilder = QueryBuilders.boolQuery();
		SearchRequestBuilder plainBuilder = client.prepareSearch(INDEX_NAME).setTypes(TYPE_NAME)
				.setQuery(QueryBuilders.boolQuery()
						.must(QueryBuilders.matchQuery("place", queryMap.get("place")))
						.must(QueryBuilders.matchQuery("category", queryMap.get("category")))
		.must(QueryBuilders.matchQuery("productType", queryMap.get("productType"))));
		AggregationBuilder aggregationType =  AggregationBuilders.terms("type").field("type"+ ".keyword");
		plainBuilder.addAggregation(aggregationType);
		SearchResponse attResponse = plainBuilder.get();
		SearchHit[] hits = attResponse.getHits().getHits();
		Terms terms = attResponse.getAggregations().get("type");
		List<String> buckets = new ArrayList<String>();
		terms.getBuckets().forEach(bucket -> {
			buckets.add(bucket.getKeyAsString());

		});
		return buckets;
	}

	@Override
	public ConsumerOffer offersSearch(GetOfferSearchQueryDTO getOfferSearchQueryDTO) {
		ProductDTO pDTO = new ProductDTO();

		SearchRequestBuilder requestAttOrderBuilder = getAttributeSearchRequestBuilder(getOfferSearchQueryDTO.getProductType());
		SearchResponse attResponse = requestAttOrderBuilder.get();
		SearchHit[] hits = attResponse.getHits().getHits();
		Attributes_Order attributes_order = new Attributes_Order();
		for(SearchHit hit : hits){
			String source =hit.getSourceAsString();
			if (source != null) {
				Gson gson = new GsonBuilder().setDateFormat("yyyMMdd")
						.create();
				attributes_order = gson.fromJson(source, Attributes_Order.class);
			}
			String sourceAsString = hit.getSourceAsString();

		}
		if (hits.length == 0) {
			logger.info("hits is zero");
			return null;
		}
		// getOrder list
		List<Entry<String, Integer>> orderList = queryUtility.createOrderList(hits[0],"order");
		Map<String,String> otherValueList = queryUtility.getAttributeValues(hits[0],"additionalValues");
		// actual order of attributes
		Map<String, Integer> order = queryUtility.getOrder(hits[0], "order");
		Map<String, Integer> range = queryUtility.getOrder(hits[0], "range");
		Map<String, Integer> importanceMap = queryUtility.getOrder(hits[0], "importance");
		SearchRequestBuilder plainQBuilder = null;
		plainQBuilder = offerQuery.createOfferQueries(getOfferSearchQueryDTO, orderList,range,importanceMap);
		plainQBuilder.setSize(3000);

		logger.info("Preparing query");

		SearchResponse response = plainQBuilder.get();
		Map<String, List<String>> facets = queryUtility.getFacets(response, orderList,hits[0]);
		pDTO.setAttributes_orders(attributes_order);
		createSearchResult(pDTO, order, response, facets,otherValueList);
		//Get Offers Alogorithm
		ConsumerOffer consumerOffer = new ConsumerOffer();

			List<GetOfferResponseDTO> searchResult = offersAlgorithm.filterProducts(pDTO.getProducts(), importanceMap, getOfferSearchQueryDTO);
			List<GetOfferResponseDTO> offerPriceResult = offersAlgorithm.filterProdcutsOnOfferPrice(searchResult, getOfferSearchQueryDTO);
			//if (!(offerPriceResult.size() > 0))
			offerPriceResult = offersAlgorithm.filterProdcutsOnMatchPrice(offerPriceResult, getOfferSearchQueryDTO);
			if (offerPriceResult.size() > 0) {
				offersAlgorithm.calculateOfferPrice(offerPriceResult, getOfferSearchQueryDTO);
				consumerOffer.setGetOfferResponseDTOList(offerPriceResult);
			}

		return consumerOffer;
	}

	@Override
	public ProductDTO retailerSearch(GetOfferSearchQueryDTO getOfferSearchQueryDTO) {
		offerQuery.createRetailerFilterQuery();
		return null;
	}


}
