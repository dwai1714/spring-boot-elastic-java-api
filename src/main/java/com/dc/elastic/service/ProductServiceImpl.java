package com.dc.elastic.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.action.admin.indices.mapping.get.GetFieldMappingsResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.transport.TransportClient;
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

import com.dc.elastic.model.Product;
import com.dc.elastic.model.ProductDTO;

@Service

public class ProductServiceImpl implements ProductService {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	@Autowired
	private TransportClient client;

	@Override
	public ProductDTO getProductDTO(String type) {
		ProductDTO pDTO = new ProductDTO();
		SearchRequestBuilder requestAttOrderBuilder = client.prepareSearch("my_ord_index").setTypes("attOrder");
		QueryBuilder attQB = QueryBuilders.boolQuery().must(QueryBuilders.matchQuery("Type", type));
		requestAttOrderBuilder.setQuery(attQB);
		SearchResponse attResponse = requestAttOrderBuilder.get();
		SearchHit[] hits = attResponse.getHits().getHits();
		Map<String, Object> attSource = hits[0].getSourceAsMap();
		Map<String, Integer> order = (Map<String, Integer>) attSource.get("order");

		Set<Entry<String, Integer>> set = order.entrySet();
		List<Entry<String, Integer>> list = new ArrayList<Entry<String, Integer>>(set);
		Collections.sort(list, new Comparator<Map.Entry<String, Integer>>() {
			public int compare(Map.Entry<String, Integer> o1, Map.Entry<String, Integer> o2) {
				return (o1.getValue()).compareTo(o2.getValue());// Ascending order
			}
		});

		logger.info("Preparing query");
		SearchRequestBuilder requestBuilder = client.prepareSearch("my_index").setTypes("products");
		AggregationBuilder aggregation = AggregationBuilders.nested("all_attributes", "attributes");

		for (Map.Entry<String, Integer> entry : list) {

			String termString =  entry.getKey();

			if (getDataType("attributes", entry.getKey()).equals("text"))
				aggregation.subAggregation(AggregationBuilders.terms( entry.getKey())
						.field("attributes." + entry.getKey() + ".keyword"));
			else
				aggregation.subAggregation(
						AggregationBuilders.terms( entry.getKey()).field("attributes." + entry.getKey()));
		}

		NestedQueryBuilder nqb = QueryBuilders.nestedQuery("attributes",
				QueryBuilders.boolQuery().must(QueryBuilders.matchQuery("attributes.Brand", "Orient")), ScoreMode.Max);
		QueryBuilder qb = QueryBuilders.boolQuery().must(QueryBuilders.matchQuery("Type", type));

		requestBuilder.setQuery(qb);
		// requestBuilder.setQuery(nqb);
		requestBuilder.addAggregation(aggregation);

		// Get response
		logger.info("Executing query");
		SearchResponse response = requestBuilder.get();

		Nested agg = response.getAggregations().get("all_attributes");
		Map<String, Map<String, Long>> facets = new HashMap<>();

		for (Map.Entry<String, Integer> entry : list) {
			Terms terms = agg.getAggregations().get( entry.getKey());
			Map<String, Long> buckets = new HashMap();
			terms.getBuckets().forEach(bucket -> {
				buckets.put(bucket.getKeyAsString(), bucket.getDocCount());

			});
			if (buckets.size() != 0)
			facets.put( entry.getKey(), buckets);

			// System.out.println(entry.getKey() + " ==== " + entry.getValue());
		}
		// Map<String, Map<String, Long>> attributes;

		hits = response.getHits().getHits();
		List<Product> products = new ArrayList<Product>();
		Arrays.asList(hits).forEach(hit -> {
			Map<String, Object> sourceObject = hit.getSourceAsMap();
			Map<String, Object> attributes = (Map<String, Object>) sourceObject.get("attributes");

			Product product = new Product();
			product.setType(sourceObject.get("Type").toString());
			product.setPlace(sourceObject.get("Place").toString());
			product.setCategory(sourceObject.get("Category").toString());
			product.setId(hit.getId());
			product.setAttributes(attributes);

			products.add(product);

		});

		logger.info("Products is " + products);

		logger.info("agg.getDocCount() is " + agg.getDocCount());
		pDTO.setProducts(products);
		pDTO.setAttributes(facets);
		pDTO.setOrder(order);
		return pDTO;
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

			logger.info("KeyString " + keyString);

		});
		logger.info("KeyString List is " + types);

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

}
