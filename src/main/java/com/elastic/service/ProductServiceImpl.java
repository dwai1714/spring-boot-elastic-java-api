package com.elastic.service;

import java.util.*;
import java.util.Map.Entry;

import com.elastic.model.Attributes_Order;
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
import org.elasticsearch.search.aggregations.bucket.MultiBucketsAggregation;
import org.elasticsearch.search.aggregations.bucket.nested.Nested;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.elastic.model.Product;
import com.elastic.model.ProductDTO;
import com.elastic.model.SearchQueryDTO;
import com.elastic.util.ExcelUtility;
import com.google.gson.Gson;
import org.springframework.util.StringUtils;

@Service

public class ProductServiceImpl implements ProductService {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	@Autowired
	private TransportClient client;
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
		List<Entry<String, Integer>> orderList = createOrderList(hits[0],"order");
		// actual order of attributes
		Map<String, Integer> order = getOrder(hits[0], "order");
		logger.info("Preparing query");
		SearchRequestBuilder requestBuilder = createProductSearchRequestBuilder(type, orderList);

		// Get response
		logger.info("Executing query");
		SearchResponse response = requestBuilder.get();

		//
		Map<String, List<String>> facets = getFacets(response, orderList,hits[0]);
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

			products.add(product);

		});
		logger.info("Query Done");
		pDTO.setProducts(products);
		pDTO.setAttributes(facets);

		addAdditionalValues(pDTO, facets, otherValueList);
		//pDTO.setOrder(order);
	}

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
	public ProductDTO getProductDTOMatchQuery(SearchQueryDTO searchQueryDTO) {


		ProductDTO pDTO = new ProductDTO();

		SearchRequestBuilder requestAttOrderBuilder = getAttributeSearchRequestBuilder(searchQueryDTO.getProductType());
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
		List<Entry<String, Integer>> orderList = createOrderList(hits[0],"order");
		Map<String,String> otherValueList = getAttributeValues(hits[0],"additionalValues");
		// actual order of attributes
		Map<String, Integer> order = getOrder(hits[0], "order");
		Map<String, Integer> range = getOrder(hits[0], "range");

		SearchRequestBuilder plainQBuilder = createQueries(searchQueryDTO, orderList,range);
		logger.info("Preparing query");

		SearchResponse response = plainQBuilder.get();
		Map<String, List<String>> facets = getFacets(response, orderList,hits[0]);
		pDTO.setAttributes_orders(attributes_order);
		createSearchResult(pDTO, order, response, facets,otherValueList);
		return pDTO;
	}

	/**
	 * This method create queries for multiple options selected and product type
	 * 
	 * @param searchQueryDTO
	 * @param orderList
	 * @param range
	 * @return
	 */
	private SearchRequestBuilder createQueries(SearchQueryDTO searchQueryDTO, List<Entry<String, Integer>> orderList, Map<String, Integer> range) {
		BoolQueryBuilder queryBuilder = QueryBuilders.boolQuery();
		if (null != searchQueryDTO.getAttributes()) {
			Set<String> keys = searchQueryDTO.getAttributes().keySet();
			for (String key : keys) {
				List<String> attributeValuesList = searchQueryDTO.getAttributes().get(key);

				if(null!=range && range.keySet().contains(key)){
					for (String value : attributeValuesList) {
						String[] values = splitValue(value);
						RangeQueryBuilder rq = QueryBuilders.rangeQuery("attributes."+key);
						if(checkNumeric(values[0])){
							rq.from(values[0]);
						}
						if(checkNumeric((values[1]))){
							rq.to(values[1]);
						}
						BoolQueryBuilder query = QueryBuilders.boolQuery();
						queryBuilder.must(QueryBuilders.nestedQuery("attributes",query.must(rq),ScoreMode.Avg));
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
					.must(QueryBuilders.matchQuery("type", searchQueryDTO.getProductType())).
							must(QueryBuilders.nestedQuery("attributes",queryBuilder,ScoreMode.Max)));
		AggregationBuilder aggregation = getAggregationBuilder(orderList);
		plainBuilder.addAggregation(aggregation);
		return plainBuilder;
	}

	/**
	 * Check the numeric
	 * @param val
	 * @return
	 */
	private boolean checkNumeric(String val) {
		return val != null && val.matches("[-+]?\\d*\\.?\\d+");
	}

	/**
	 * Splits the value
	 * @param value
	 * @return
	 */
	private String[] splitValue(String value) {
		return value.split("-");
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

	/**
	 * This method will buiuld factes/aggragations
	 *
	 * @param response
	 * @param orderList
	 * @return
	 */
	private Map<String, List<String>> getFacets(SearchResponse response, List<Entry<String, Integer>> orderList,SearchHit searchHit) {
		Map<String, Integer> order = getOrder(searchHit, "range");
		Map<String, Integer> min = getOrder(searchHit, "min");
		Map<String, Integer> max = getOrder(searchHit, "max");

		Map<String, List<String>> facets = new HashMap<String, List<String>>();
		Nested agg = response.getAggregations().get("all_attributes");
		for (Map.Entry<String, Integer> entry : orderList) {
			MultiBucketsAggregation terms;
			terms = agg.getAggregations().get(entry.getKey());

			List<String> buckets = new ArrayList<String>();
			terms.getBuckets().forEach(bucket -> {
				buckets.add(bucket.getKeyAsString());

			});
			if (buckets.size() != 0)
				facets.put(entry.getKey(), buckets);

		}
		reOrderFacets(facets,order,min,max);
		return facets;

	}

	private void reOrderFacets(Map<String, List<String>> facets, Map<String, Integer> order, Map<String, Integer> min, Map<String, Integer> max) {
		reOrderWithRange(facets, order);
		reOrderPrice(facets, min, max);
	}

	private void reOrderPrice(Map<String, List<String>> facets, Map<String, Integer> min, Map<String, Integer> max) {
		Set<String> minKeys = min.keySet();
		for(String facetKey:minKeys){
			Integer minValue =min.get(facetKey);
			Integer maxValue = max.get(facetKey);
			if(minValue>0 && maxValue>0) {
				List<String> minMax = new ArrayList<String>();
				minMax.add(minValue + "-" + maxValue);
				facets.put(facetKey, minMax);
			}
		}
	}

	private void reOrderWithRange(Map<String, List<String>> facets, Map<String, Integer> order) {
		Set<String> attributes = order.keySet();
		for(String attribute:attributes){
			List<String> reOrderList = facets.get(attribute);
			if(null!=reOrderList && reOrderList.size()>0) {
				List<String> resultList = createRangeFacet(reOrderList, order, attribute);
				facets.put(attribute, resultList);
			}
		}
	}

	private List<String> createRangeFacet(List<String> reOrderList, Map<String, Integer> order, String attribute) {
		List<String> resultList = new ArrayList<String>();
		if(order.get(attribute)>0){
			List<String> text = removeTextValues(reOrderList);
			int value = order.get(attribute);
             getSortedList(reOrderList);
            String current="";
            String prev = "";
            int count =1;
            boolean flag = false;
            for(String key:reOrderList){
                current = key;
                if(count==1){
                    prev = key;
                }
                if(count!=1 && (value%count==0)){
                    resultList.add(prev+"-"+current);
                    prev=current;
                    count=1;
                    flag = true;
                }else {
                    count++;
                    flag = false;
                }
            }
            if(!flag){
                resultList.add(current+"-"+"above");

            }
			resultList.addAll(text);
			return resultList;
		}else{
			return reOrderList;
		}

	}

	private void getSortedList(List<String> reOrderList) {
		//for()
		 Collections.sort(reOrderList, new Comparator<String>() {
			@Override
			public int compare(String first, String second) {
				int firstValue = Math.round(Float.parseFloat(first));
				int secondValue = Math.round(Float.parseFloat(second));
				return Integer.compare(firstValue,secondValue);

			}
		});
		// return reOrderList;
	}

	private List<String> removeTextValues(List<String> reOrderList) {
		List<String> text = new ArrayList<String>();
		if(null!=reOrderList && reOrderList.size()>0) {
			Iterator<String> itr = reOrderList.iterator();
			while (itr.hasNext()) {
				String current = "";
				try {
					current = itr.next();
					Float.parseFloat(current);
				} catch (NumberFormatException ne) {
					text.add(current);
					itr.remove();
				}
			}
		}
		return text;
	}


	/**
	 * This method will create the order list
	 *
	 * @param hit
	 * @return
	 */
	private List<Entry<String, Integer>> createOrderList(SearchHit hit,String metaData) {
		Map<String, Integer> order = getOrder(hit,metaData);

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
	 * @param metaData
	 * @return
	 */
	private Map<String, Integer> getOrder(SearchHit hit, String metaData) {
		Map<String, Object> attSource = hit.getSourceAsMap();
		Map<String,Map<String,Object>> metaDataMap = (Map<String,Map<String,Object>>)attSource.get("attributes_metadata");
		Set<String> attributes = metaDataMap.keySet();
		return createOrderingList(attributes,metaDataMap,metaData);
		}
	private Map<String, String> getAttributeValues(SearchHit hit, String metaData) {
		Map<String, Object> attSource = hit.getSourceAsMap();
		Map<String,Map<String,Object>> metaDataMap = (Map<String,Map<String,Object>>)attSource.get("attributes_metadata");
		Set<String> attributes = metaDataMap.keySet();
		return createOtherValuesList(attributes,metaDataMap,metaData);
		//return null;
	}


	private Map<String, Integer> createOrderingList(Set<String> attributes, Map<String, Map<String, Object>> mappedValues, String metaData) {
		Map<String,Integer> orderList = new HashMap<String,Integer>();
		for(String name:attributes){
			Map<String,Object> indMap = (Map<String,Object>)mappedValues.get(name);
			int order =0;
			if(!StringUtils.isEmpty(indMap.get(metaData))){
				order  = Integer.parseInt((String)indMap.get(metaData));
			}
			orderList.put(name,order);

		}
		return orderList;
	}
	private Map<String, String> createOtherValuesList(Set<String> attributes, Map<String, Map<String, Object>> mappedValues, String metaData) {
		Map<String,String> orderList = new HashMap<String,String>();
		for(String name:attributes){
			Map<String,Object> indMap = (Map<String,Object>)mappedValues.get(name);
			int order =0;
			if(!StringUtils.isEmpty(indMap.get(metaData))){
				orderList.put(name,(String)indMap.get(metaData));

			}

		}
		return orderList;
	}

}
