package com.novillex.progresstracker.entity;

import lombok.Data;

@Data
public class PaymentSystems {

    private Boolean rtgs;

    private Boolean neft;

    private Boolean imps;

    private Boolean upi;

    private Boolean nach;

    private Boolean bbps;

    private Boolean aeps;

    private Boolean rupay;

    private Boolean atmSwitch;

    private Boolean pos;

    private Boolean reconciliation;

    private Boolean aml;

    private Integer dailyUpiTransactions;

    private Integer dailyImpsTransactions;

    private Integer dailyNeftTransactions;

    private Integer dailyRtgsTransactions;
}
