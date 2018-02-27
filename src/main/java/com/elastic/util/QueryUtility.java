package com.elastic.util;

import com.elastic.model.Product;
import com.elastic.dto.ProductDTO;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.aggregations.bucket.MultiBucketsAggregation;
import org.elasticsearch.search.aggregations.bucket.nested.Nested;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.*;

/**
 * This class is created for utility mehtods for query creations.
 */
@Component
public class QueryUtility {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    /**
     * Check the numeric
     * @param val
     * @return
     */
    public boolean checkNumeric(String val) {
        return val != null && val.matches("[-+]?\\d*\\.?\\d+");
    }

    /**
     * Splits the value
     * @param value
     * @return
     */
    public String[] splitValue(String value) {
        return value.split("-");
    }

    /**
     * This method will create the search result from the search response
     *  @param pDTO
     * @param order
     * @param response
     * @param facets
     * @param otherValueList
     */
    private List<Product> createSearchResult(ProductDTO pDTO, Map<String, Integer> order, SearchResponse response,
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
        return products;
       // pDTO.setProducts(products);
//        pDTO.setAttributes(facets);

        //addAdditionalValues(pDTO, facets, otherValueList);
        //pDTO.setOrder(order);
    }

    /**
     * This method will buiuld factes/aggragations
     *
     * @param response
     * @param orderList
     * @return
     */
    public Map<String, List<String>> getFacets(SearchResponse response, List<Map.Entry<String, Integer>> orderList, SearchHit searchHit) {
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
    /**
     * This method will get the raw order which needs to be processed further
     *
     * @param hit
     * @param metaData
     * @return
     */
    public Map<String, Integer> getOrder(SearchHit hit, String metaData) {
        Map<String, Object> attSource = hit.getSourceAsMap();
        Map<String,Map<String,Object>> metaDataMap = (Map<String,Map<String,Object>>)attSource.get("attributes_metadata");
        Set<String> attributes = metaDataMap.keySet();
        return createOrderingList(attributes,metaDataMap,metaData);
    }

    /**
     * This method will give attribute values
     *
     * @param hit
     * @param metaData
     * @return
     */
    public Map<String, String> getAttributeValues(SearchHit hit, String metaData) {
        Map<String, Object> attSource = hit.getSourceAsMap();
        Map<String,Map<String,Object>> metaDataMap = (Map<String,Map<String,Object>>)attSource.get("attributes_metadata");
        Set<String> attributes = metaDataMap.keySet();
        return createOtherValuesList(attributes,metaDataMap,metaData);
        //return null;
    }

    /**
     *
     * @param attributes
     * @param mappedValues
     * @param metaData
     * @return
     */
    public Map<String, Integer> createOrderingList(Set<String> attributes, Map<String, Map<String, Object>> mappedValues, String metaData) {
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

    /**
     *
     * @param attributes
     * @param mappedValues
     * @param metaData
     * @return
     */
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


    /**
     * This method will reorder facets
     * @param facets
     * @param order
     * @param min
     * @param max
     */
    private void reOrderFacets(Map<String, List<String>> facets, Map<String, Integer> order, Map<String, Integer> min, Map<String, Integer> max) {
        reOrderWithRange(facets, order);
        reOrderPrice(facets, min, max);
    }

    /**
     * This method will reorder the price
     * @param facets
     * @param min
     * @param max
     */
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

    /**
     * This method will reorder with range
     * @param facets
     * @param order
     */
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

    /**
     * This method will create range facets
     * @param reOrderList
     * @param order
     * @param attribute
     * @return
     */
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

    /**
     * This method will sort the list
     * @param reOrderList
     */
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

    /**
     * This method will create the order list
     *
     * @param hit
     * @return
     */
    public List<Map.Entry<String, Integer>> createOrderList(SearchHit hit, String metaData) {
        Map<String, Integer> order = getOrder(hit,metaData);

        Set<Map.Entry<String, Integer>> set = order.entrySet();
        List<Map.Entry<String, Integer>> list = new ArrayList<Map.Entry<String, Integer>>(set);
        Collections.sort(list, new Comparator<Map.Entry<String, Integer>>() {
            public int compare(Map.Entry<String, Integer> o1, Map.Entry<String, Integer> o2) {
                return (o1.getValue()).compareTo(o2.getValue());// Ascending order
            }
        });
        return list;
    }
    /**
     * This method will remove text values.
     * @param reOrderList
     * @return
     */
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


}
