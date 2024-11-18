package com.serenity.integration.serenity;

import java.time.ZonedDateTime;

import lombok.Getter;
import lombok.Setter;
@Setter
@Getter
public class MedicalRequest {
private String uuid;
    private ZonedDateTime createdAt;
    private ZonedDateTime updatedAt;
    private ZonedDateTime authoredOn;
    private String name;
    private String category;
    private String code;
    private ZonedDateTime date;
    private String notes;
    private String intendedDispenser;
    private String priority;
    private String status;
    private String dosageDisplay;
    private String dosageForm;
    private String dosageRoute;
    private String dosageSite;
    private String dosageFrequency;
    private String dosageFrequencyUnit;
    private double dose;
    private String doseUnit;
    private String dosageStrength;
    private String dosagePeriod;
    private String courseOfTherapy;
    private int quantityToDispense;
    private int numberOfRefills;
    private String dosagePeriodUnit;
    private String serviceProviderId;
    private String visitId;
    private String patientId;
    private String patientMrNumber;
    private String patientFullName;
    private String practitionerName;
    private String practitionerId;
}
