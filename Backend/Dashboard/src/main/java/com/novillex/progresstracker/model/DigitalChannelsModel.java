package com.novillex.progresstracker.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class DigitalChannelsModel {
	private Boolean mobileBanking;

    private Boolean internetBanking;

    private Boolean tabletBanking;

    private Boolean whatsAppBanking;

    private Boolean missedCallBanking;

    private Boolean smsBanking;
    
    @JsonProperty("eStatement")
    private Boolean eStatement;

    private Boolean debitCardServices;

    private Integer mobileUsers;

    private Integer internetUsers;

    private Integer cardUsers;

    private Integer activeDigitalUsers;
    
}
