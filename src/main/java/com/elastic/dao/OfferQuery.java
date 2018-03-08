package com.elastic.dao;

import com.elastic.constants.QueryConstants;
import com.elastic.dto.SearchQueryDTO;
import com.elastic.util.QueryUtility;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.action.admin.indices.mapping.get.GetFieldMappingsResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
/**
 * This class will handle query creation and execution logic in elastic server
 */
public class OfferQuery {
    @Autowired
    private TransportClient client;
    @Autowired
    QueryUtility queryUtility;

    public  SearchRequestBuilder createOfferQueries(SearchQueryDTO searchQueryDTO, List<Map.Entry<String, Integer>> orderList, Map<String, Integer> range, Map<String, Integer> importance) {
        BoolQueryBuilder queryBuilder = QueryBuilders.boolQuery();
        RangeQueryBuilder rq = null;
        if (null != searchQueryDTO.getAttributes()) {
            Set<String> keys = searchQueryDTO.getAttributes().keySet();
            for (String key : keys) {
                List<String> attributeValuesList = searchQueryDTO.getAttributes().get(key);

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
                    BoolQueryBuilder insideQuery = QueryBuilders.boolQuery();
                    if (null != attributeValuesList) {
                        for (String value : attributeValuesList) {
                          {
                                insideQuery.should(QueryBuilders.matchQuery("attributes." + key, value));
                            }
                        }
                        createOuterQuery(key,insideQuery,importance,queryBuilder);
                    } //else if
                }//else
            }
        }
        SearchRequestBuilder plainBuilder = client.prepareSearch(QueryConstants.INDEX_NAME).setTypes(QueryConstants.TYPE_NAME);
        plainBuilder.setQuery(QueryBuilders.boolQuery()
                .must(QueryBuilders.matchQuery("place", searchQueryDTO.getPlace()))
                .must(QueryBuilders.nestedQuery("attributes",
                        QueryBuilders.existsQuery("attributes.Sale Price"),ScoreMode.Max))
                .must(QueryBuilders.matchQuery("category", searchQueryDTO.getCategory()))
                .must(QueryBuilders.matchQuery("type", searchQueryDTO.getProductType())).
                must(QueryBuilders.nestedQuery("attributes",queryBuilder, ScoreMode.Max)));
        AggregationBuilder aggregation = getAggregationBuilder(orderList);
        plainBuilder.addAggregation(aggregation);
        createRetailerFilterQuery();
        return plainBuilder;
    }

    /**
     * This method will create retailer query
     */
    public void createRetailerFilterQuery() {
        SearchRequestBuilder plainBuilder = client.prepareSearch(QueryConstants.INDEX_NAME_RETAILER).setTypes(QueryConstants.TYPE_NAME_RETAILER);
        plainBuilder.setQuery(QueryBuilders.boolQuery()
                .must(QueryBuilders.matchQuery("retialerId", "5a93e32d8998e6452cb9ed44"))
                .should(QueryBuilders.nestedQuery("deliveryTiers.deliveryMethods",
                        QueryBuilders.matchQuery("deliveryMethodName",
                                "Express: 3 to 5 business days"),ScoreMode.Avg)));
        SearchResponse response = plainBuilder.get();
       // plainBuilder.get();
        System.out.println(response);

    }

    /**
     * This method will check the importance and creates must or should outer query.
     * @param key
     * @param insideQuery
     * @param importance
     * @param queryBuilder
     */
    private void  createOuterQuery(String key,BoolQueryBuilder insideQuery, Map<String, Integer> importance,
                                   BoolQueryBuilder queryBuilder) {
        if(null!=importance && importance.get(key)==100){
            queryBuilder.must(insideQuery);
        }else{
            queryBuilder.should(insideQuery);
        }
    }

    /**
     * This method will build the aggregation builder with all the attributes
     *
     * @param orderList
     * @return
     */
    private AggregationBuilder getAggregationBuilder(List<Map.Entry<String, Integer>> orderList) {
        AggregationBuilder aggregation = AggregationBuilders.nested("all_attributes", "attributes");

        for (Map.Entry<String, Integer> entry : orderList) {

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
     * This method will return the data type of specific field from attributes order.
     * @param nestedField
     * @param field
     * @return
     */

    public String getDataType(String nestedField, String field) {

        GetFieldMappingsResponse response = client.admin().indices().prepareGetFieldMappings().addIndices(QueryConstants.INDEX_NAME)
                .addTypes(QueryConstants.TYPE_NAME).setFields(nestedField + "." + field).get();

        Map<String, Object> fieldSource;
        try {
            fieldSource = response.fieldMappings(QueryConstants.INDEX_NAME, QueryConstants.TYPE_NAME, nestedField + "." + field).sourceAsMap();
        } catch (Exception e) {
            // Bad Code but will figure later
            return "text";
        }

        String typeIs = (String) ((LinkedHashMap) (fieldSource.get(field))).get("type");
        return typeIs;

    }



}
