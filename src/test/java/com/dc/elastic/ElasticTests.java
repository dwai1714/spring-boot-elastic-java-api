package com.dc.elastic;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.lucene.search.join.ScoreMode;
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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

@RunWith(SpringRunner.class)
@SpringBootTest
public class ElasticTests {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	@Autowired
	private TransportClient client;

	@Test
	public void doBasicTest() {
		logger.info("Preparing query");
		SearchRequestBuilder requestBuilder = client.prepareSearch("my_index").setTypes("products");
		AggregationBuilder aggregation = AggregationBuilders.nested("all_attributes", "attributes");
		
		aggregation.subAggregation(AggregationBuilders.terms("popular_Fan_Size").field("attributes.Fan Size"));		
		aggregation.subAggregation(AggregationBuilders.terms("popular_Fan_Color").field("attributes.Fan Color.keyword"));
		aggregation.subAggregation(AggregationBuilders.terms("popular_Fan_Display").field("attributes.Display Technology.keyword"));
		

		NestedQueryBuilder nqb = QueryBuilders.nestedQuery("attributes",
				QueryBuilders.boolQuery().must(QueryBuilders.matchQuery("attributes.Brand", "LG")).
				should(QueryBuilders.matchQuery("attributes.Brand", "Orient")),
				ScoreMode.Max);
		QueryBuilder qb = QueryBuilders.boolQuery().must(QueryBuilders.matchQuery("Type", "TV's Refrigerator")).must(nqb);
		

		requestBuilder.setQuery(qb);
		//requestBuilder.setQuery(nqb);
		requestBuilder.addAggregation(aggregation);

		// Get response
		logger.info("Executing query");
		SearchResponse response = requestBuilder.get();

		// Terms terms = response.getAggregations().get("all_attributes");
		Nested agg = response.getAggregations().get("all_attributes");
		Terms fanSize = agg.getAggregations().get("popular_Fan_Size");
		Terms fanColor = agg.getAggregations().get("popular_Fan_Display");
	
		SearchHit[] hits = response.getHits().getHits();
		Arrays.asList(hits).forEach(hit -> {
			Map<String, Object> source = hit.getSourceAsMap(); 
			Object object = source.get("attributes");
			logger.info("Source Object is " + source); // Source Object
		//	logger.info("Attributes are " + object); // Attributes
			// score when the score mode is the default(average)
			logger.info("Score by average: " + hit.getScore());
		});

		//Fan Size Bucket - Just want the key and value
		fanSize.getBuckets().forEach(bucket -> {
	        String keyString = bucket.getKeyAsString();
	        long docCount = bucket.getDocCount();
	        logger.info("Color KeyString is " + keyString + " docCount is " + docCount);

		});	
		
		//Fan Color Bucket - Just want the key and value

		fanColor.getBuckets().forEach(bucket -> {
	        String keyString = bucket.getKeyAsString();
	        long docCount = bucket.getDocCount();
	        logger.info("Size KeyString is " + keyString + " docCount is " + docCount);

		});		
		
		logger.info("agg.getDocCount() is " + agg.getDocCount());
	}

}
