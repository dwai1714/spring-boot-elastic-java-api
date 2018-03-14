package com.elastic.util;

import com.elastic.constants.QueryConstants;
import com.elastic.dto.GetOfferResponseDTO;
import com.elastic.model.Product;
import com.elastic.dto.GetOfferSearchQueryDTO;
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
     * @param getOfferSearchQueryDTO
     * @return
     */
    public List<GetOfferResponseDTO> filterProducts(List<Product> products,
                                                    Map<String, Integer> importanceMap,
                                                    GetOfferSearchQueryDTO getOfferSearchQueryDTO) {
        List<GetOfferResponseDTO> getOfferResponseDTOList =  new ArrayList<GetOfferResponseDTO>();
        Map<String,List<String>> attributes = getOfferSearchQueryDTO.getAttributes();
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
                GetOfferResponseDTO getOfferResponseDTO =
                        new GetOfferResponseDTO(product,totalScore);
                getOfferResponseDTO.setDeliveryMethod((String)product.getAttributes().get("DeliveryMethod"));
                getOfferResponseDTO.setRetailerName("Best Buy");
                getOfferResponseDTOList.add(getOfferResponseDTO);
            }
        }
        Collections.sort(getOfferResponseDTOList,new ProductScoreComparator(QueryConstants.SCORE));
        return getOfferResponseDTOList;

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
         if(!attributes.get(key).contains("No Preference")) {
             if ("Features".equalsIgnoreCase(key)) {
                 count = count + ((List<String>) attributes.get(key)).size();
             } else {
                 count++;
             }
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
                    if (actualValue.toLowerCase().contains(inputValue.toLowerCase()) || inputValue.toLowerCase().contains(actualValue.toLowerCase())) {
                        score = importanceMap.get(key);
                        break;
                    }
                }
            } else {
                for (String inputValue : inputValues) {
                    inputValue = inputValue.toLowerCase();
                    for(String actual:actualValueList){
                        actual = actual.toLowerCase();
                        if (actual.contains(inputValue)
                                || (inputValue).contains(actual))
                            score += importanceMap.get(key);
                    }
                    }

            }
        }
        return score;
       }

    /**
     * This method will filter the products depending on offer price formula
     * @param searchResult
     * @param getOfferSearchQueryDTO
     */
    public List<GetOfferResponseDTO> filterProdcutsOnOfferPrice(List<GetOfferResponseDTO> searchResult, GetOfferSearchQueryDTO getOfferSearchQueryDTO) {
        List<GetOfferResponseDTO> offerPriceProducts = new ArrayList<GetOfferResponseDTO>();
        for(GetOfferResponseDTO getOfferResponseDTO :searchResult){
            Product product = getOfferResponseDTO.getProduct();
            Map<String,Object> attributes = product.getAttributes();
            if(null!=attributes.get("Lowest Make An Offer Price")){
                calculateDiffConsumerAndRetialerOfferPrice(getOfferSearchQueryDTO, offerPriceProducts, getOfferResponseDTO, attributes);
            }
        }//for loop end

        return offerPriceProducts;
    }

    /**
     * This method will calculate offer price .
     * @param getOfferSearchQueryDTO
     * @param offerPriceProducts
     * @param getOfferResponseDTO
     * @param attributes
     */
    private void calculateDiffConsumerAndRetialerOfferPrice(GetOfferSearchQueryDTO getOfferSearchQueryDTO, List<GetOfferResponseDTO> offerPriceProducts, GetOfferResponseDTO getOfferResponseDTO, Map<String, Object> attributes) {
        Double retailerMinOfferPrice = Double.valueOf((String)attributes.get("Lowest Make An Offer Price"));
        Double maxConsumerPrice = getOfferSearchQueryDTO.getPrice().getMaxPrice();
            getOfferResponseDTO.setDifference(maxConsumerPrice-retailerMinOfferPrice);
            offerPriceProducts.add(getOfferResponseDTO);
        }

    /**
     * This method will filter the prodcts depends on match price formula
     * @param searchResult
     * @param getOfferSearchQueryDTO
     * @return
     */
    public List<GetOfferResponseDTO> filterProdcutsOnMatchPrice(List<GetOfferResponseDTO> searchResult,
                                                                GetOfferSearchQueryDTO getOfferSearchQueryDTO) {
        List<GetOfferResponseDTO> matchPriceProducts = new ArrayList<GetOfferResponseDTO>();
        for(GetOfferResponseDTO getOfferResponseDTO :searchResult){
            Product product = getOfferResponseDTO.getProduct();
            Map<String,Object> attributes = product.getAttributes();
            if(null!=attributes.get("Lowest Make An Offer Price")){
                calculateMatchPrice(getOfferSearchQueryDTO, matchPriceProducts, getOfferResponseDTO, attributes);
            }
        }//for loop end
        Collections.sort(matchPriceProducts,new ProductScoreComparator(QueryConstants.MATCH_PRICE));

        return matchPriceProducts;


    }

    /**
     * This method will calcualte match price
     * @param getOfferSearchQueryDTO
     * @param matchPriceProducts
     * @param getOfferResponseDTO
     * @param attributes
     */
    private void calculateMatchPrice(GetOfferSearchQueryDTO getOfferSearchQueryDTO, List<GetOfferResponseDTO> matchPriceProducts,
                                     GetOfferResponseDTO getOfferResponseDTO, Map<String, Object> attributes) {
        Double retailerMinOfferPrice = Double.valueOf((String)attributes.get("Lowest Make An Offer Price"));
        Double maxConsumerPrice = getOfferSearchQueryDTO.getPrice().getMaxPrice();
        Double matchFactorScore = getOfferResponseDTO.getScore();
        Double calcualteFactorScore = matchFactorScore*kalaCoefficientFactor;
        Double priceMatchScore = (maxConsumerPrice-retailerMinOfferPrice)+calcualteFactorScore;
            getOfferResponseDTO.setPriceMatchScore(new BigDecimal(priceMatchScore).setScale(2,BigDecimal.ROUND_HALF_UP).doubleValue());
            matchPriceProducts.add(getOfferResponseDTO);
     }

    /**
     * This method will calculate for each of the product going to be offered
     * @param offerPriceResult
     * @param getOfferSearchQueryDTO
     */
    public void calculateOfferPrice(List<GetOfferResponseDTO> offerPriceResult,
                                    GetOfferSearchQueryDTO getOfferSearchQueryDTO) {
        for(GetOfferResponseDTO getOfferResponseDTO :offerPriceResult){
            Map<String,Object> attributes = getOfferResponseDTO.getProduct().getAttributes();
            Double productPrice = Double.valueOf  ((String)attributes.get("Sale Price"));
            Double minRetailerOfferPrice = Double.valueOf((String)attributes.get("Lowest Make An Offer Price"));
            Double offerPrice = new Double(0);
          if((productPrice> getOfferSearchQueryDTO.getPrice().getMaxPrice() )
                  && getOfferSearchQueryDTO.getPrice().getMaxPrice()>minRetailerOfferPrice ){
               offerPrice = minRetailerOfferPrice
                      +((getOfferSearchQueryDTO.getPrice().getMaxPrice()-minRetailerOfferPrice)
                      *(getOfferSearchQueryDTO.getPrice().getMinPrice()/ getOfferSearchQueryDTO.getPrice().getMaxPrice()));
          }else if((productPrice> getOfferSearchQueryDTO.getPrice().getMaxPrice() ) &&
                  getOfferSearchQueryDTO.getPrice().getMaxPrice()<minRetailerOfferPrice){
               offerPrice = minRetailerOfferPrice
                      +((productPrice-minRetailerOfferPrice)/xKalaCoefficientFactor)
                      *((productPrice- getOfferSearchQueryDTO.getPrice().getMaxPrice())/yKalaCoefficientFactor);

          }else if((getOfferSearchQueryDTO.getPrice().getMaxPrice()<productPrice)
                  && (getOfferSearchQueryDTO.getPrice().getMaxPrice()==minRetailerOfferPrice)){
              offerPrice = productPrice-(yKalaCoefficientFactor*(productPrice- getOfferSearchQueryDTO.getPrice().getMinPrice()));
          }else if(getOfferSearchQueryDTO.getPrice().getMaxPrice()>productPrice){
              offerPrice = productPrice;
          }
          getOfferResponseDTO.setOfferPrice(new BigDecimal(offerPrice).setScale(2,BigDecimal.ROUND_HALF_UP).doubleValue());
        }//for
    Collections.sort(offerPriceResult,new ProductScoreComparator(QueryConstants.OFFER_PRICE));
    }
}
