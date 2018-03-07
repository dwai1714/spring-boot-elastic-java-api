package com.elastic.util;

import com.elastic.constants.QueryConstants;
import com.elastic.dto.ProductScoreDTO;
import com.elastic.model.Product;
import com.elastic.dto.SearchQueryDTO;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;

@Component
/**
 * This class handles all the loic for offers algorithm
 */
public class OffersAlgorithm {
    private final double thresholdValue = 0.40;
    private final double kalaCoefficientFactor = 200;
    private final double xKalaCoefficientFactor = 10;

    private final double yKalaCoefficientFactor = 50;

    public List<Product> getProductList() {
        return productList;
    }

    public void setProductList(List<Product> productList) {
        this.productList = productList;
    }

    List<Product> productList;


    /**
     * This method will filter the results
     * @param products
     * @param importanceMap
     * @param searchQueryDTO
     * @return
     */
    public List<ProductScoreDTO> filterProducts(List<Product> products,
                                        Map<String, Integer> importanceMap,
                                        SearchQueryDTO searchQueryDTO) {
        List<ProductScoreDTO> productScoreDTOList =  new ArrayList<ProductScoreDTO>();
        Map<String,List<String>> attributes = searchQueryDTO.getAttributes();
        Set<String> keys = attributes.keySet();
        for(Product product:products) {
             Double totalScore =new Double(0);
            Integer count = new Integer(0);
            for (String key : keys) {
                if (importanceMap.get(key) != 100) {
                    double score = getIndividualScore(attributes, importanceMap, key, product);
                    totalScore = totalScore + score;
                    count = incrementCount(count, key, attributes, importanceMap);
                }//if
            }//for
             totalScore = totalScore/(count*100);
            if(totalScore>thresholdValue){
                ProductScoreDTO productScoreDTO =
                        new ProductScoreDTO(product,totalScore);
                productScoreDTOList.add(productScoreDTO);
            }
        }
        Collections.sort(productScoreDTOList,new ProductScoreComparator(QueryConstants.SCORE));
        return productScoreDTOList;

    }

    /**
     * This method will increment count
     * @param count
     * @param key
     * @param attributes
     * @param importanceMap
     * @return
     */
    private Integer incrementCount(Integer count, String key, Map<String, List<String>> attributes, Map<String, Integer> importanceMap) {
           if ("Features".equalsIgnoreCase(key)) {
               count = count + ((List<String>) attributes.get(key)).size();
           } else {
               count++;
           }

        return count;
    }


    /**
     *This method will calculate the score
     * @param inputAttributes
     * @param importanceMap
     * @param key
     * @param product
     * @return
     */
    private double getIndividualScore(Map<String, List<String>> inputAttributes, Map<String, Integer> importanceMap,
                                      String key, Product product) {
        Map<String, Object> attributes = product.getAttributes();
        List<String> inputValues = inputAttributes.get(key);
        boolean isFeatures = false;
        String actualValue = null;
        List<String> actualValueList = null;
        if("Features".equalsIgnoreCase(key)){
            isFeatures = true;
            actualValueList = (List<String>)attributes.get(key);

        }else{
            actualValue = (String)attributes.get(key);
        }
       // String actualValue = (String)attributes.get(key);
        double score = 0;
        if(null!=inputValues && inputValues.size()>0) {
            if (!isFeatures) {
                for (String inputValue : inputValues) {
                    if (actualValue.contains(inputValue) || inputValue.contains(actualValue)) {
                        score = importanceMap.get(key);
                        break;
                    }
                }
            } else {
                for (String inputValue : inputValues) {
                    if (actualValueList.contains(inputValue))
                            score += importanceMap.get(key);
                   }
            }
        }
        return score;
       }

    /**
     * This method will filter the products depending on offer price formula
     * @param searchResult
     * @param searchQueryDTO
     */
    public List<ProductScoreDTO> filterProdcutsOnOfferPrice(List<ProductScoreDTO> searchResult, SearchQueryDTO searchQueryDTO) {
        List<ProductScoreDTO> offerPriceProducts = new ArrayList<ProductScoreDTO>();
        for(ProductScoreDTO productScoreDTO:searchResult){
            Product product = productScoreDTO.getProduct();
            Map<String,Object> attributes = product.getAttributes();
            if(null!=attributes.get("Lowest Make An Offer Price")){
                calculateDiffConsumerAndRetialerOfferPrice(searchQueryDTO, offerPriceProducts, productScoreDTO, attributes);
            }
        }//for loop end

        return offerPriceProducts;
    }

    /**
     * This method will calculate offer price .
     * @param searchQueryDTO
     * @param offerPriceProducts
     * @param productScoreDTO
     * @param attributes
     */
    private void calculateDiffConsumerAndRetialerOfferPrice(SearchQueryDTO searchQueryDTO, List<ProductScoreDTO> offerPriceProducts, ProductScoreDTO productScoreDTO, Map<String, Object> attributes) {
        Double retailerMinOfferPrice = Double.valueOf((String)attributes.get("Lowest Make An Offer Price"));
        Double maxConsumerPrice = searchQueryDTO.getMaxOfferPrice();
            productScoreDTO.setDifference(maxConsumerPrice-retailerMinOfferPrice);
            offerPriceProducts.add(productScoreDTO);
        }

    /**
     * This method will filter the prodcts depends on match price formula
     * @param searchResult
     * @param searchQueryDTO
     * @return
     */
    public List<ProductScoreDTO> filterProdcutsOnMatchPrice(List<ProductScoreDTO> searchResult,
                                                            SearchQueryDTO searchQueryDTO) {
        List<ProductScoreDTO> matchPriceProducts = new ArrayList<ProductScoreDTO>();
        for(ProductScoreDTO productScoreDTO:searchResult){
            Product product = productScoreDTO.getProduct();
            Map<String,Object> attributes = product.getAttributes();
            if(null!=attributes.get("Lowest Make An Offer Price")){
                calculateMatchPrice(searchQueryDTO, matchPriceProducts, productScoreDTO, attributes);
            }
        }//for loop end
        Collections.sort(matchPriceProducts,new ProductScoreComparator(QueryConstants.MATCH_PRICE));

        return matchPriceProducts;


    }

    /**
     * This method will calcualte match price
     * @param searchQueryDTO
     * @param matchPriceProducts
     * @param productScoreDTO
     * @param attributes
     */
    private void calculateMatchPrice(SearchQueryDTO searchQueryDTO, List<ProductScoreDTO> matchPriceProducts,
                                     ProductScoreDTO productScoreDTO, Map<String, Object> attributes) {
        Double retailerMinOfferPrice = Double.valueOf((String)attributes.get("Lowest Make An Offer Price"));
        Double maxConsumerPrice = searchQueryDTO.getMaxOfferPrice();
        Double matchFactorScore = productScoreDTO.getScore();
        Double calcualteFactorScore = matchFactorScore*kalaCoefficientFactor;
        Double priceMatchScore = (maxConsumerPrice-retailerMinOfferPrice)+calcualteFactorScore;
            productScoreDTO.setPriceMatchScore(new BigDecimal(priceMatchScore).setScale(2,BigDecimal.ROUND_HALF_UP).doubleValue());
            matchPriceProducts.add(productScoreDTO);
     }

    /**
     * This method will calculate for each of the product going to be offered
     * @param offerPriceResult
     * @param searchQueryDTO
     */
    public void calculateOfferPrice(List<ProductScoreDTO> offerPriceResult,
                                    SearchQueryDTO searchQueryDTO) {
        for(ProductScoreDTO productScoreDTO:offerPriceResult){
            Map<String,Object> attributes = productScoreDTO.getProduct().getAttributes();
            Double productPrice = Double.valueOf  ((String)attributes.get("Sale Price"));
            Double minRetailerOfferPrice = Double.valueOf((String)attributes.get("Lowest Make An Offer Price"));
            Double offerPrice = new Double(0);
          if((productPrice>searchQueryDTO.getMaxOfferPrice() )
                  && searchQueryDTO.getMaxOfferPrice()>minRetailerOfferPrice ){
               offerPrice = minRetailerOfferPrice
                      +((searchQueryDTO.getMaxOfferPrice()-minRetailerOfferPrice)
                      *(searchQueryDTO.getMinOfferPrice()/searchQueryDTO.getMaxOfferPrice()));
          }else if((productPrice>searchQueryDTO.getMaxOfferPrice() ) &&
                  searchQueryDTO.getMaxOfferPrice()<minRetailerOfferPrice){
               offerPrice = minRetailerOfferPrice
                      +((productPrice-minRetailerOfferPrice)/xKalaCoefficientFactor)
                      *((productPrice-searchQueryDTO.getMaxOfferPrice())/yKalaCoefficientFactor);

          }else if((searchQueryDTO.getMaxOfferPrice()<productPrice)
                  && (searchQueryDTO.getMaxOfferPrice()==minRetailerOfferPrice)){
              offerPrice = productPrice-(yKalaCoefficientFactor*(productPrice-searchQueryDTO.getMinOfferPrice()));
          }else if(searchQueryDTO.getMaxOfferPrice()>productPrice){
              offerPrice = productPrice;
          }
          productScoreDTO.setOfferPrice(new BigDecimal(offerPrice).setScale(2,BigDecimal.ROUND_HALF_UP).doubleValue());
        }//for
    Collections.sort(offerPriceResult,new ProductScoreComparator(QueryConstants.OFFER_PRICE));
    }
}
