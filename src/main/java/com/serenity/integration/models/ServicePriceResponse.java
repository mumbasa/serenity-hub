package com.serenity.integration.models;

import java.util.List;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Setter
@Getter
@ToString
public class ServicePriceResponse {
private List<ServicePricing> data;
private int total;
}
