package com.novillex.progresstracker.entity;

import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class DigitalChannels {

	private Boolean mobileBanking;

	private Boolean internetBanking;

	private Boolean tabletBanking;

	private Boolean pigmyBanking;
	
	@Min(value = 0, message = "Mobile Banking Users cannot be negative")
	private Integer mobileUsers;
	
	@Min(value = 0, message = "Internet Banking Users cannot be negative")
    private Integer internetUsers;
	
	@Min(value = 0, message = "Debit Card Users cannot be negative")
    private Integer cardUsers;
	
	@Min(value = 0, message = "Active Digital Users cannot be negative")
    private Integer activeDigitalUsers;
}
