package com.serenity.integration.serenity;

import java.time.ZonedDateTime;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class ServiceRequest {

    private String uuid;
    private ZonedDateTime createdAt;
    private ZonedDateTime updatedAt;
    private String accessionNumber;
    private List<String> bodySite;
    private String display;
    private String notes;
    private String category;
    private String code;
    private String diagnosticServiceSection;
    private ZonedDateTime dueDate;
    private String purpose;
    private String passportNumber;
    private ZonedDateTime sampleReceivedDateTime;
    private String priority;
    private String healthcareServiceId;
    private String healthcareServiceName;
    private String chargeItemId;
    private String priceTierId;
    private String replacesId;
    private String status;
    private String statusReason;
    private String groupIdentifier;
    private String intent;
    private boolean doNotPerform;
    private boolean isPaid;
    private double charge;
    private String practitionerName;
    private String encounterClass;
    private boolean paymentRequired;
    private String patientMrNumber;
    private String patientId;
    private String patientMobile;
    private String patientBirthDate;
    private String patientGender;
    private String patientFullName;
    private ZonedDateTime occurence;
    private String practitionerId;
}
