package com.novillex.progresstracker.model;

import lombok.Data;

@Data
public class PaymentSystemsModel {
	private Boolean rtgs;

    private Boolean neft;

    private Boolean imps;

    private Boolean atmSwitch;

    private Boolean pos;

    private Boolean loanRecovery;

    private Integer dailyAtmTransactions;

    private Integer dailyImpsTransactions;

    private Integer dailyNeftTransactions;

    private Integer dailyRtgsTransactions;
}
