package com.elastic.service;

import static org.elasticsearch.index.query.QueryBuilders.boolQuery;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.action.admin.indices.mapping.get.GetFieldMappingsResponse;
import org.elasticsearch.action.search.MultiSearchResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.NestedQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.nested.Nested;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.elastic.model.Product;
import com.elastic.model.ProductDTO;
import com.elastic.model.SearchQueryDTO;

@Service

public class ProductServiceImpl implements ProductService {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	@Autowired
	private TransportClient client;

	@Override
	public ProductDTO getProductDTO(String type) {
		ProductDTO pDTO = new ProductDTO();
		SearchRequestBuilder requestAttOrderBuilder = getAttributeSearchRequestBuilder(type);
		//check attributes order
		SearchResponse attResponse = requestAttOrderBuilder.get();
		SearchHit[] hits = attResponse.getHits().getHits();
		if (hits.length == 0) {
			return pDTO;
		}

		//getOrder list
		List<Entry<String, Integer>> orderList = createOrderList(hits[0]);
		//actual order of attributes
		Map<String, Integer> order = getOrder(hits[0]);


		logger.info("Preparing query");
		SearchRequestBuilder requestBuilder = createProductSearchRequestBuilder(type,orderList);

		// Get response
		logger.info("Executing query");
		SearchResponse response = requestBuilder.get();

		//
		Map<String, Map<String, Long>> facets = getFacets(response,orderList);


		createSearchResult(pDTO, order, response, facets);
		return pDTO;
	}

	/**
	 * This method will create the search result from the search response
	 * @param pDTO
	 * @param order
	 * @param response
	 * @param facets
	 */
	private void createSearchResult(ProductDTO pDTO, Map<String, Integer> order, SearchResponse response, Map<String, Map<String, Long>> facets) {
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
		pDTO.setOrder(order);
	}

	/**
	 * This method will buiuld factes/aggragations
	 * @param response
	 * @param orderList
	 * @return
	 */
	private Map<String,Map<String,Long>> getFacets(SearchResponse response, List<Entry<String, Integer>> orderList) {
		Map<String, Map<String, Long>> facets = new HashMap<String,Map<String,Long>>();
		Nested agg = response.getAggregations().get("all_attributes");
		for (Map.Entry<String, Integer> entry : orderList) {
			Terms terms = agg.getAggregations().get(entry.getKey());
			Map<String, Long> buckets = new HashMap();
			terms.getBuckets().forEach(bucket -> {
				buckets.put(bucket.getKeyAsString(), bucket.getDocCount());

			});
			if (buckets.size() != 0)
				facets.put(entry.getKey(), buckets);

		}
		return facets;

	}

	/**
	 * This method will create product search request builder with aggregations for single critera.
	 * @param type
	 * @param orderList
	 * @return
	 */
	private SearchRequestBuilder createProductSearchRequestBuilder(String type, List<Entry<String, Integer>> orderList) {
		SearchRequestBuilder requestBuilder = client.prepareSearch("my_index").setTypes("products");
		AggregationBuilder aggregation = getAggregationBuilder(orderList);

		NestedQueryBuilder nqb = QueryBuilders.nestedQuery("attributes",
				QueryBuilders.boolQuery().must(QueryBuilders.matchQuery("attributes.Brand", "Orient")), ScoreMode.Max);
		QueryBuilder qb = QueryBuilders.boolQuery().must(QueryBuilders.matchQuery("type", type));

		requestBuilder.setQuery(qb);
		// requestBuilder.setQuery(nqb);
		requestBuilder.addAggregation(aggregation);
		return requestBuilder;
	}

	/**
	 * This method will build the aggregation builder with all the attributes
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
	 * @param hit
	 * @return
	 */
	private List<Entry<String,Integer>> createOrderList(SearchHit hit) {
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
 	 * @param hit
	 * @return
	 */
	private Map<String, Integer> getOrder(SearchHit hit) {
		Map<String, Object> attSource = hit.getSourceAsMap();
		return (Map<String, Integer>) attSource.get("order");
	}

	/**
	 *Attributes search query builder
	 * @param type
	 * @return
	 */
	private SearchRequestBuilder getAttributeSearchRequestBuilder(String type) {
		SearchRequestBuilder requestAttOrderBuilder = client.prepareSearch("my_ord_index").setTypes("attOrder");
		QueryBuilder attQB = QueryBuilders.boolQuery().must(QueryBuilders.matchQuery("Type", type));
		requestAttOrderBuilder.setQuery(attQB);
		return requestAttOrderBuilder;
	}

	@Override
	public ProductDTO getProductDTOFullText(String fullText) {
		SearchRequestBuilder searchqueryBuilder = client.prepareSearch("my_index").setTypes("products");
		SearchRequestBuilder PlainBuilder = client.prepareSearch("my_index").setTypes("products")
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
	 * This method will be used to get the multi search response for full text search.
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
		SearchRequestBuilder requestBuilder = client.prepareSearch("my_index").setTypes("products");
		SearchResponse response = requestBuilder
				.addAggregation(AggregationBuilders.terms("by_types").field("Type.keyword")).execute().actionGet();
		Terms terms = response.getAggregations().get("by_types");
		List types = new ArrayList();
		terms.getBuckets().forEach(bucket -> {
			String keyString = bucket.getKeyAsString();
			types.add(keyString);

		});

		return types;

	}

	public String getDataType(String nestedField, String field) {

		GetFieldMappingsResponse response = client.admin().indices().prepareGetFieldMappings().addIndices("my_index")
				.addTypes("products").setFields(nestedField + "." + field).get();

		Map<String, Object> fieldSource;
		try {
			fieldSource = response.fieldMappings("my_index", "products", nestedField + "." + field).sourceAsMap();
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
		if (hits.length == 0) {
			logger.info("hits is zero");
			return pDTO;
		}
		//getOrder list
		List<Entry<String, Integer>> orderList = createOrderList(hits[0]);
		//actual order of attributes
		Map<String, Integer> order = getOrder(hits[0]);
		SearchRequestBuilder plainQBuilder = createQueries(searchQueryDTO,orderList);
		logger.info("Preparing query");

		SearchResponse response = plainQBuilder.get();
		Map<String, Map<String, Long>> facets = getFacets(response,orderList);


		createSearchResult(pDTO, order, response, facets);
		return pDTO;

	}

	/**
	 * This method create queries for multiple options selected  and product type
	 * @param searchQueryDTO
	 * @param orderList
	 * @return
	 */
	private SearchRequestBuilder createQueries(SearchQueryDTO searchQueryDTO, List<Entry<String, Integer>> orderList) {
		BoolQueryBuilder queryBuilder = QueryBuilders.boolQuery();

		if(null!=searchQueryDTO.getAttributes()) {
			Set<String> keys = searchQueryDTO.getAttributes().keySet();
			for (String key : keys) {
				List<String> attributeValuesList = searchQueryDTO.getAttributes().get(key);
				if (null != attributeValuesList && searchQueryDTO.getAttributes().size() > 0) {
					for (String value : attributeValuesList) {
						queryBuilder.must(QueryBuilders.nestedQuery("attributes",
								QueryBuilders.matchQuery("attributes." + key, value), ScoreMode.Avg));

					}
				}

			}
		}
		SearchRequestBuilder plainBuilder = client.prepareSearch("my_index").setTypes("products")
				.setQuery(QueryBuilders.boolQuery()
						.must(QueryBuilders.matchQuery("type", searchQueryDTO.getProductType()))
						.must(queryBuilder));

		AggregationBuilder aggregation = getAggregationBuilder(orderList);
		plainBuilder.addAggregation(aggregation);

		return plainBuilder;

	}



}
