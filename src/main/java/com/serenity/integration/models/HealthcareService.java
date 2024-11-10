package com.serenity.integration.models;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;
import com.serenity.integration.setup.Location;
import com.serenity.integration.setup.Ward;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
@Setter
@Getter
@ToString
public class HealthcareService implements Serializable {

    @SerializedName("is_active")
    private boolean isActive;
   @SerializedName("healthcare_service_appointment_required")
    private boolean healthcareServiceAppointmentRequired;
   @SerializedName("virtual_service")
    private boolean virtual_service;
    private String provider;

   @SerializedName("healthcare_service_locations")
    private List<Location> healthcareServiceLocations;

   @SerializedName("order_code")
    private String orderCode;

   @JsonProperty("healthcare_service_name")
    private String healthcareServiceName;

   @JsonProperty("service_request_category")
    private String serviceRequestCategory;

   @SerializedName("diagnostic_service_section")
    private String diagnosticServiceSection;
    private String characteristic;
    private String comment;
    private String communication;

   @SerializedName("extra_details")
    private String extraDetails;
    private String telecom;
    private String program;


    @JsonProperty("maximum_capacity")
    private int  maximumCapacity;
    
  @SerializedName("revenue_display_tag")
    private String  revenueDisplayTag;

    @JsonProperty("ward_type")
    private String wardType;

    @JsonProperty("subscription_frequency")
    private String subscriptionFrequency;
    
   @JsonProperty("referral_method")
    private List<String> referralMethod;

    @JsonProperty("uuid")
    private String uuid;

    private List<AvailableTime> healthcareServiceAvailableTimes;
   @SerializedName("healthcare_service_categories")
    private List<Category> healthcareServiceCategories;
    private List<NotAvailableTime> healthcareServiceNotAvailableTimes;
   @SerializedName("price_tiers")
    private List<PriceTier> priceTiers;
   @SerializedName("slot_duration")
    private String slotDuration;
   @SerializedName("healthcare_service_service_provision_code")
    private String healthcareServiceServiceProvisionCode;   
    @SerializedName("healthcare_service_specialties")
    private List<Specialty> healthcareServiceSpecialties;
   @SerializedName("healthcare_service_types")
    private List<ServiceType> healthcareServiceTypes;
    
 
    public HealthcareService(){

    } 

    public HealthcareService(Ward ward){

      HealthcareService service = new HealthcareService();
                            this.setHealthcareServiceName(ward.getWardName());
                            Category category = new Category();
                            category.setCode("Hospitalization");
                            List<Category> cats = new ArrayList<>();
                            cats.add(category);
                            this.setHealthcareServiceCategories(cats);
    
                            Specialty specialty = new Specialty();
                            specialty.setCode("Adult_mental_illness");
                            List<Specialty> specs = new ArrayList<>();
                            specs.add(specialty);
                            this.setHealthcareServiceSpecialties(specs);
    
                            ServiceType serviceType = new ServiceType();
                            serviceType.setCode("General Care");
                            serviceType.setDisplay("General Care");;
                            List<ServiceType> serviceTypes = new ArrayList<>();
                            serviceTypes.add(serviceType);
                            this.setHealthcareServiceTypes(serviceTypes);
                            this.setProvider("161380e9-22d3-4627-a97f-0f918ce3e4a9");
    
                            this.setSlotDuration("30");
                            this.setVirtual_service(false);
                            this.setHealthcareServiceAppointmentRequired(true);
                            this.setComment("Healthcare service for General Consultation at Nyaho Medical Center");
                            this.setExtraDetails(ward.getPrice());
                            this.setMaximumCapacity(10);
                            this.setRevenueDisplayTag("Other");
                            this.setSubscriptionFrequency("Daily");
                            this.setWardType(ward.getWardType());
                            this.setHealthcareServiceServiceProvisionCode("cost");

                            PriceTier free = new PriceTier();
                            free.setCharge("0");
                            free.setDisplay("Free");
                            free.setCurrency("GHS");
                            free.setDescription("Free service for " +  ward.getWardName());
                            free.setPriority("routine");
    
                            PriceTier priceTier = new PriceTier();
                            priceTier.setCharge(ward.getPrice());
                            priceTier.setCurrency("GHS");
                            priceTier.setDisplay("Express");
                            priceTier.setPriority("urgent");
                            priceTier.setDisplay("Express");
                            priceTier.setDescription("Express service for " + ward.getWardName());
    
                            PriceTier standard = new PriceTier();
                            standard.setCharge("50");
                            standard.setDisplay("Standard");
                            standard.setCurrency("GHS");
                            standard.setDescription("Standard service for " +  ward.getWardName());
                            standard.setPriority("routine");
    
                            List<PriceTier> priceTiers = new ArrayList<>();
                            priceTiers.add(priceTier);
                            priceTiers.add(free);
                            priceTiers.add(standard);
                            this.setPriceTiers(priceTiers);
    } 

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
