package com.serenity.integration.models;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class PractitionerResponse {

    private boolean success;    
    private Practitioner data ;
    private String message;
}
