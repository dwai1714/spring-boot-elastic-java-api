package com.elastic.service;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.Map.Entry;

import org.apache.lucene.search.join.ScoreMode;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.elasticsearch.action.admin.indices.mapping.get.GetFieldMappingsResponse;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
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
import org.springframework.util.StringUtils;

@Service

public class ProductServiceImpl implements ProductService {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	@Autowired
	private TransportClient client;
	 List<String> styles = new ArrayList<String>();
	 List<String> bowlSaped = new ArrayList<String>();
	 List<String> height = new ArrayList<String>();
	 List<String> brand = new ArrayList<String>();
	 List<String> features = new ArrayList<String>();
	 List<String> types = new ArrayList<String>();

	 List<String> featuresBidets = new ArrayList<String>();
	 List<String> featuresUrinals = new ArrayList<String>();
	 List<String> selector12 = new ArrayList<String>();
	 List<String> selector13 = new ArrayList<String>();
	 List<String> selector14 = new ArrayList<String>();
	 List<String> selector15 = new ArrayList<String>();
	 List<String> selector16 = new ArrayList<String>();

	static List<String> colors = new ArrayList<String>();


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
		//Nested agg = response.getAggregations().get("all_attributes");
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
		//AggregationBuilder aggregation = AggregationBuilders.significantTerms("type").field("type"+".keyword");
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

	@Override
	public void readExcel() throws Exception {
		ArrayList<HashMap<String, Object>> productList = new ArrayList<HashMap<String, Object>>();
				initializeMaps();
			int count =0;
			for(String type:types){
				if(count==0) {
					createToiletProducts(type,productList);
					}
				else if(count==1){
				createBidetsProducts(type,productList);
			}else if(count==2){
				createUrinalProducts(type,productList);
			}else{
			createAccessoriesProducts(type,productList);
			}
			count++;
	}
		int productCount = 1;
		BulkRequestBuilder brb = client.prepareBulk();
		for(HashMap<String,Object> product:productList){
				if(productCount<5000){
					brb.add(client.prepareIndex("my_index", "products").setSource(product));
					productCount++;
				}else{
					BulkResponse bulkResponse = brb.execute().actionGet();
					brb = null;
					brb = client.prepareBulk();
					productCount = 1;
				}
		}
		BulkResponse bulkResponse = brb.execute().actionGet();

	}

	private void initializeMaps() throws Exception {
		InputStream ExcelFileToRead = new FileInputStream("D:\\shravan\\files\\Book1.xlsx");
		XSSFWorkbook wb = new XSSFWorkbook(ExcelFileToRead);
		Set<Integer> elements = new HashSet<Integer>();
		ArrayList<HashMap<String,Object>> hMap = new ArrayList<HashMap<String,Object>>();
		XSSFSheet sheet = wb.getSheetAt(0);
		XSSFRow row;
		XSSFCell cell;

		Iterator rows = sheet.rowIterator();
		int rowCount = 0;

		//loop through rows
		while (rows.hasNext())
        {
            row = (XSSFRow) rows.next();
            if(rowCount==0||rowCount==1){
                rowCount++;
                continue;
            }
            Iterator cells = row.cellIterator();
            int col =0;
            while (cells.hasNext()) {
                cell = (XSSFCell) cells.next();
                if(col<13){
                    createProduct(cell,cell.getColumnIndex());
                }
                col++;
            }//whil1e
            rowCount++;

        }//while
	//	return hMap;
	}

	private  void createAccessoriesProducts(String type,ArrayList<HashMap<String,Object>> alProducts) {
        for(String selector:selector12) {
            for (String feature : featuresBidets) {
                for (String color : colors) {
                    for (String brand : brand) {
                        HashMap<String, Object> map = new HashMap<String, Object>();
                        HashMap<String, Object> hashMap = createBaseMap(type);
                        map.put("Brand", brand);
                        map.put("Features", feature);
                        map.put("Color", color);
                        map.put("PartsFor", selector);

                        hashMap.put("attributes", map);
                        alProducts.add(hashMap);
                    }
                }
            }
        }
	}

	private  HashMap<String, Object> createBaseMap(String type) {
		HashMap<String,Object> hashMap = new HashMap<String,Object>();
		hashMap.put("place","Home&Garden");
		hashMap.put("category","BathRoom");
		hashMap.put("productType","Toilets");
		hashMap.put("type",type);
		return hashMap;
	}

	private  void createBidetsProducts(String type,ArrayList<HashMap<String,Object>> alProducts) {
			for(String feature:featuresBidets){
				for(String color:colors) {
					for(String brand:brand){
						HashMap<String,Object> map = new HashMap<String,Object>();
						HashMap<String, Object> hashMap = createBaseMap(type);
						map.put("Brand",brand);
						map.put("Features",feature);
						map.put("Color",color);
						hashMap.put("attributes",map);
						alProducts.add(hashMap);
					}
				}
			}
		}

		private  void createUrinalProducts(String type,ArrayList<HashMap<String,Object>> alProducts) {
			for(String feature:featuresUrinals){
				for(String color:colors) {
					for(String brand:brand){
						HashMap<String,Object> map = new HashMap<String,Object>();
						HashMap<String, Object> hashMap = createBaseMap(type);
						map.put("Brand",brand);
						map.put("Features",feature);
						map.put("Color",color);
						hashMap.put("attributes",map);
						alProducts.add(hashMap);
					}
				}
			}
		}

		private  void createToiletProducts(String type,ArrayList<HashMap<String,Object>> alProducts) {
			for(String style:styles){
				for(String bowl:bowlSaped) {
					for(String height:height){
						for(String feature:features){
							for(String brands:brand){
								for(String color:colors){
									HashMap<String,Object> map = new HashMap<String,Object>();
									HashMap<String, Object> hashMap = createBaseMap(type);
									map.put("Style",style);
									map.put("Brand",brands);
									map.put("Features",feature);
									map.put("Color",color);
									map.put("Shape",bowl);
									map.put("Part Of Height",height);
									hashMap.put("attributes",map);
									alProducts.add(hashMap);

								}

							}
						}
					}
				}
			}

			//return alProducts;
		}

		private  void createProduct(XSSFCell cell, int col) throws Exception{


			if(!StringUtils.isEmpty(cell.getStringCellValue())) {
				switch (col) {
					case 0:
						types.add(cell.getStringCellValue());
						break;
					case 1:
						styles.add(cell.getStringCellValue());
						break;
					case 2:
						bowlSaped.add(cell.getStringCellValue());
						break;
					case 3:
						height.add(cell.getStringCellValue());
						break;
					case 4:
						features.add(cell.getStringCellValue());
						break;
					case 5:
						featuresBidets.add(cell.getStringCellValue());
						break;
					case 6:
						featuresUrinals.add(cell.getStringCellValue());
						break;
					case 7:
						colors.add(cell.getStringCellValue());
						break;
					case 8:
						selector12.add(cell.getStringCellValue());
						break;
					case 9:
						selector13.add(cell.getStringCellValue());
						break;
					case 10:
						selector14.add(cell.getStringCellValue());
						break;
					case 11:
						selector15.add(cell.getStringCellValue());
						break;
					case 12:
						brand.add(cell.getStringCellValue());
						break;

				}
			}

		}






	/**
	 * This method create queries for multiple options selected  and product type
	 * @param searchQueryDTO
	 * @param orderList
	 * @return
	 */
	private SearchRequestBuilder createQueries(SearchQueryDTO searchQueryDTO, List<Entry<String, Integer>> orderList) {
		BoolQueryBuilder queryBuilder = QueryBuilders.boolQuery();

		/*if(null!=searchQueryDTO.getAttributes()) {
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
		}*/
		SearchRequestBuilder plainBuilder = client.prepareSearch("my_index").setTypes("products")
				.setQuery(QueryBuilders.boolQuery()
						.must(QueryBuilders.matchQuery("type", searchQueryDTO.getProductType())))
                ;
			//			.must(queryBuilder));

		AggregationBuilder aggregation = getAggregationBuilder(orderList);
		plainBuilder.addAggregation(aggregation);

		return plainBuilder;

	}



}
