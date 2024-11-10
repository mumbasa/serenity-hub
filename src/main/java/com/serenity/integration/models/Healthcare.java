package com.serenity.integration.models;

import java.io.Serializable;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;
import com.serenity.integration.setup.Location;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
@Setter
@Getter
@ToString
public class Healthcare implements Serializable {

    @JsonProperty("is_active")
    private boolean isActive;
    @JsonProperty("id")
    private String id;
   @JsonProperty("healthcare_service_appointment_required")
    private boolean healthcareServiceAppointmentRequired;
   @JsonProperty("virtual_service")
    private boolean virtual_service;
    private String provider;

   @JsonProperty("healthcare_service_locations")
    private List<Location> healthcareServiceLocations;

   @JsonProperty("order_code")
    private String orderCode;

   @JsonProperty("healthcare_service_name")
    private String healthcareServiceName;

   @JsonProperty("service_request_category")
    private String serviceRequestCategory;

   @JsonProperty("diagnostic_service_section")
    private String diagnosticServiceSection;
    private String characteristic;
    private String comment;
    private String communication;
   @JsonProperty("extra_details")
    private String extraDetails;
    private String telecom;
    private String program;
   @JsonProperty("referral_method")
    private List<String> referralMethod;
    private List<AvailableTime> healthcareServiceAvailableTimes;
   @JsonProperty("healthcare_service_categories")
    private List<Category> healthcareServiceCategories;
    private List<NotAvailableTime> healthcareServiceNotAvailableTimes;
   @JsonProperty("price_tiers")
    private List<PriceTier> priceTiers;
   @JsonProperty("slot_duration")
    private String slotDuration;
   @JsonProperty("healthcare_service_service_provision_code")
    private String healthcareServiceServiceProvisionCode;   
    @JsonProperty("healthcare_service_specialties")
    private List<Specialty> healthcareServiceSpecialties;
   @JsonProperty("healthcare_service_types")
    private List<ServiceType> healthcareServiceTypes;
    
    
    
    @Setter
    @Getter
    @ToString
    public static class NotAvailableTime {
        private String description;
        private String startDate;
        private String endDate;
        
        // Getters and Setters
    }
   
   
   
}
