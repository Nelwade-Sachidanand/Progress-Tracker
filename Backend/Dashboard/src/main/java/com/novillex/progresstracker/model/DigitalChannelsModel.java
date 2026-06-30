package com.novillex.progresstracker.model;

import lombok.Data;

@Data
public class DigitalChannelsModel {
	private Boolean mobileBanking;

	private Boolean internetBanking;

	private Boolean tabletBanking;

	private Boolean pigmyBanking;
	
	private Integer mobileUsers;

    private Integer internetUsers;

    private Integer cardUsers;

    private Integer activeDigitalUsers;
    
}
