package com.serenity.integration.models;

import java.util.UUID;

import org.apache.commons.csv.CSVRecord;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Setter
@Getter
@ToString
public class ServicePricing {

    @JsonProperty("name")
    private String name;

    @JsonProperty("description")
    private String description;

    @JsonProperty("amount")
    private String amount;

    @JsonProperty("amount_type")
    private String amountType;

    @JsonProperty("currency")
    private String currency;

    @JsonProperty("healthcare_service_id")
    private String healthcareServiceId;

    @JsonProperty("healthcare_service_name")
    private String healthcareServiceName;

    @JsonProperty("customer_group_id")
    private String customerGroupId;

    @JsonProperty("customer_group_name")
    private String customerGroupName;

    @JsonProperty("location_id")
    private String locationId;

    @JsonProperty("location_name")
    private String locationName;

    @JsonProperty("nationality")
    private String nationality;

    @JsonProperty("payer_id")
    private String payerId;

    @JsonProperty("payer_name")
    private String payerName;

    @JsonProperty("payment_method")
    private String paymentMethod;

    @JsonProperty("priority")
    private String priority;

    @JsonProperty("managing_organization")
    private String managingOrganization;

    @JsonProperty("uuid")
    private String uuid;

    @JsonProperty("created_by_id")
    private String createdById;

    @JsonProperty("created_by_name")
    private String createdByName;

    @JsonProperty("validity_period_start")
    private String validityPeriodStart;


    @JsonProperty("validity_period_end")
    private String validityPeriodEnd;

    public ServicePricing (CSVRecord record){
        this.uuid=UUID.randomUUID().toString();
        this.amount=record.get(4);
        this.amountType="price";
        this.name=record.get(0);
        this.currency=record.get(3);
        this.createdById="8aaf05f8-741e-4e66-86df-a595f981d963";
        this.createdByName="Rejoice Hormeku";
        if(!record.get(5).isEmpty() ){
        this.customerGroupName=record.get(5);
        }
        this.healthcareServiceName=record.get(1);
        this.description="Service pricing for "+name;
        this.priority="urgent";

    }

    public ServicePricing (){
        
    }
}
