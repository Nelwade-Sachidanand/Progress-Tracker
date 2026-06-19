package com.novillex.progresstracker.model;

import lombok.Data;

@Data
public class ContactModel {

    private PersonContactModel chairman;

    private PersonContactModel ceo;

    private PersonContactModel consultant;

    private PersonContactModel itHead;
}