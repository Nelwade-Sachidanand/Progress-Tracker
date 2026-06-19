package com.novillex.progresstracker.model;

import lombok.Data;

@Data
public class ChannelsModel {

    private Boolean mobileBanking;

    private Boolean netBanking;

    private Boolean tabletBanking;

    private Boolean rtgs;

    private Boolean neft;

    private Boolean atm;

    private Boolean upi;

    private Boolean imps;

    private Boolean nach;

    private Boolean bbps;

    private Boolean aml;

    private Boolean los;

    private Boolean audit;
}