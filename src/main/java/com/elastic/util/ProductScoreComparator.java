package com.elastic.util;

import com.elastic.constants.QueryConstants;
import com.elastic.dto.GetOfferResponseDTO;

import java.util.Comparator;

/**
 * This class will sort the products depends on the compare flag
 */
public class ProductScoreComparator implements Comparator<GetOfferResponseDTO> {
    private String compareFlag;

    public ProductScoreComparator(String compareFlag) {
        this.compareFlag = compareFlag;
    }

    @Override
    public int compare(GetOfferResponseDTO firstGetOfferResponseDTO, GetOfferResponseDTO secondGetOfferResponseDTO) {
        if(compareFlag.equals(QueryConstants.MATCH_PRICE)){
            return secondGetOfferResponseDTO.getPriceMatchScore().compareTo(firstGetOfferResponseDTO.getPriceMatchScore());
        }else if(compareFlag.equals(QueryConstants.SCORE)){
            return secondGetOfferResponseDTO.getScore().compareTo(firstGetOfferResponseDTO.getScore());
        }else if(compareFlag.equals(QueryConstants.OFFER_PRICE)){
            return secondGetOfferResponseDTO.getOfferPrice().compareTo(firstGetOfferResponseDTO.getOfferPrice());
        }
        return 0;
    }
}
