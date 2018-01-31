package com.dc.elastic;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

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
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import com.dc.elastic.service.ProductService;

@RunWith(SpringRunner.class)
@SpringBootTest
public class ElasticTests {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	@Autowired
	private TransportClient client;
	
	@Autowired
	private ProductService service;

	@Test
	public void doBasicTest() {
		SearchRequestBuilder requestAttOrderBuilder = client.prepareSearch("my_ord_index").setTypes("attOrder");
		QueryBuilder attQB = QueryBuilders.boolQuery().must(QueryBuilders.matchQuery("Type", "Fans"));
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

			String termString = "popular_" + entry.getValue();

			if (getDataType("attributes", entry.getKey()).equals("text"))
				aggregation.subAggregation(AggregationBuilders.terms("popular_" + entry.getValue())
						.field("attributes." + entry.getKey() + ".keyword"));
			else
				aggregation.subAggregation(
						AggregationBuilders.terms("popular_" + entry.getValue()).field("attributes." + entry.getKey()));
		}

		NestedQueryBuilder nqb = QueryBuilders.nestedQuery("attributes",
				QueryBuilders.boolQuery().must(QueryBuilders.matchQuery("attributes.Brand", "Orient")), ScoreMode.Max);
		QueryBuilder qb = QueryBuilders.boolQuery().must(QueryBuilders.matchQuery("Type", "Fans"));//.must(nqb);

		requestBuilder.setQuery(qb);
		// requestBuilder.setQuery(nqb);
		requestBuilder.addAggregation(aggregation);

		// Get response
		logger.info("Executing query");
		SearchResponse response = requestBuilder.get();

		Nested agg = response.getAggregations().get("all_attributes");

		for (Map.Entry<String, Integer> entry : list) {
			Terms terms = agg.getAggregations().get("popular_" + entry.getValue());
			terms.getBuckets().forEach(bucket -> {
				String keyString = bucket.getKeyAsString();
				long docCount = bucket.getDocCount();
				logger.info("KeyString " + entry.getKey() + " is " + keyString + " docCount is " + docCount);

			});

			// System.out.println(entry.getKey() + " ==== " + entry.getValue());
		}

		hits = response.getHits().getHits();
		Arrays.asList(hits).forEach(hit -> {
			Map<String, Object> sourceObject = hit.getSourceAsMap();
			Object attributesObject = sourceObject.get("attributes");
			logger.info("Source Object is " + sourceObject); // Source Object
			logger.info("Score by average: " + hit.getScore());
		});

		logger.info("agg.getDocCount() is " + agg.getDocCount());
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

	@Test
	public void doTypeTest() {
		getDataType("attributes", "Fan Feature");
	}

	@Test
	public void getAllTypeTest() {
		SearchRequestBuilder requestBuilder = client.prepareSearch("my_index").setTypes("products");
		SearchResponse response = requestBuilder
				.addAggregation(AggregationBuilders.terms("by_types").field("Type.keyword")).execute().actionGet();
			Terms terms = response.getAggregations().get("by_types");
			List types = new ArrayList();
			terms.getBuckets().forEach(bucket -> {
				String keyString = bucket.getKeyAsString();
				types.add(keyString);

				logger.info("KeyString " + keyString );

			});
			logger.info("KeyString List is " + types );

	}

	@Test
	public void getFullTextTest() {
		service.getProductDTOFullText("Software");

	}

}
