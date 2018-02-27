package com.elastic.util;

import com.elastic.constants.QueryConstants;
import com.elastic.dto.ProductScoreDTO;
import com.elastic.model.Product;
import com.elastic.dto.SearchQueryDTO;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class OffersAlgorithm {
    private final double thresholdValue = 0.70;
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
        Double totalScore =new Double(0);
        for(Product product:products) {
            int count = 0;
            for (String key : keys) {
               double score = getIndividualScore(attributes,importanceMap,key,product);
                totalScore =  totalScore+score;
               count++;
            }
            totalScore = totalScore/(count*100);
            if(totalScore>thresholdValue){
                ProductScoreDTO productScoreDTO =
                        new ProductScoreDTO(product,totalScore);
                productScoreDTOList.add(productScoreDTO);
            }
        }
        Collections.sort(productScoreDTOList,new ProductScoreComparator(QueryConstants.OFFER_PRICE));
        return productScoreDTOList;

    }

    /**
     *
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
        String actualValue = (String)attributes.get(key);
        if(null!=inputValues && inputValues.size()>0 &&
                inputValues.contains(actualValue)){
            return importanceMap.get(key);
        }else{
            return 0;
        }
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
        if(maxConsumerPrice-retailerMinOfferPrice>0){
            productScoreDTO.setPriceMatchScore(maxConsumerPrice-retailerMinOfferPrice);
            offerPriceProducts.add(productScoreDTO);
        }
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
        if(priceMatchScore>0){
            productScoreDTO.setPriceMatchScore(priceMatchScore);
            matchPriceProducts.add(productScoreDTO);
        }

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
            Double productPrice = Double.valueOf  ((String)attributes.get("Retail Price"));
            Double minRetailerOfferPrice = Double.valueOf((String)attributes.get("Lowest Make An Offer Price"));
            Double offerPrice = new Double(0);
          if((productPrice>searchQueryDTO.getMaxOfferPrice() )
                  && searchQueryDTO.getMaxOfferPrice()>minRetailerOfferPrice ){
               offerPrice = minRetailerOfferPrice
                      +(searchQueryDTO.getMaxOfferPrice()-searchQueryDTO.getMinOfferPrice())
                      *(searchQueryDTO.getMinOfferPrice()/searchQueryDTO.getMaxOfferPrice());
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
          productScoreDTO.setOfferPrice(offerPrice);
        }//for

    }
}
