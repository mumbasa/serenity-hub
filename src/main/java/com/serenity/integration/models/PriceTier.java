package com.serenity.integration.models;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Setter
@Getter
@ToString
public  class PriceTier {
    private String display;
    private String charge;
    private String currency;
    private String description;
    private String priority;
    
    // Getters and Setters
}