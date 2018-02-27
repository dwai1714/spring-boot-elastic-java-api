package com.elastic.util;

import com.elastic.constants.QueryConstants;
import com.elastic.dto.ProductScoreDTO;

import java.util.Comparator;

/**
 * This class will sort the products depends on the compare flag
 */
public class ProductScoreComparator implements Comparator<ProductScoreDTO> {
    private String compareFlag;

    public ProductScoreComparator(String compareFlag) {
        this.compareFlag = compareFlag;
    }

    @Override
    public int compare(ProductScoreDTO firstProductScoreDTO, ProductScoreDTO secondProductScoreDTO) {
        if(compareFlag.equals(QueryConstants.MATCH_PRICE)){
            return secondProductScoreDTO.getPriceMatchScore().compareTo(firstProductScoreDTO.getPriceMatchScore());
        }else if(compareFlag.equals(QueryConstants.OFFER_PRICE)){
            return secondProductScoreDTO.getScore().compareTo(firstProductScoreDTO.getScore());
        }
        return 0;
    }
}
