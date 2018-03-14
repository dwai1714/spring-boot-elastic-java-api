package com.elastic.dto;

import java.util.List;

public class ConsumerOffer {
    public ConsumerOffer() {
    }

    private List<GetOfferResponseDTO> getOfferResponseDTOList;

    public ConsumerOffer(List<GetOfferResponseDTO> getOfferResponseDTOList, GetOfferSearchQueryDTO getOffersRequestDTO) {
        this.getOfferResponseDTOList = getOfferResponseDTOList;
        this.getOffersRequestDTO = getOffersRequestDTO;
    }

    private GetOfferSearchQueryDTO getOffersRequestDTO;

    public List<GetOfferResponseDTO> getGetOfferResponseDTOList() {
        return getOfferResponseDTOList;
    }

    public void setGetOfferResponseDTOList(List<GetOfferResponseDTO> getOfferResponseDTOList) {
        this.getOfferResponseDTOList = getOfferResponseDTOList;
    }

    public GetOfferSearchQueryDTO getGetOffersRequestDTO() {
        return getOffersRequestDTO;
    }

    public void setGetOffersRequestDTO(GetOfferSearchQueryDTO getOffersRequestDTO) {
        this.getOffersRequestDTO = getOffersRequestDTO;
    }
}
