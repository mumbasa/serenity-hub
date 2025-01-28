package com.serenity.integration.models;

import java.time.ZonedDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Table
@Entity
public class MedicalRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;
    private String patientId;
    private String encounterId;
    private String authoredOn;
    private String name;
    private String category;
    private String code;
    private String serviceProviderName;
    private String notes;
    private String serviceProviderId;
    private String priority;
    private String status;
    private String dosageDisplay;
    private String dosageForm;
    private String dosageRoute;
    private String dosageSite;
    private String dosageFrequency;
    private String dosageFrequencyUnit;
    private Double dose;
    private String doseUnit;
    private String dosageStrength;
    private String dosagePeriod;
    private String courseOfTherapy;
    private Integer quantityToDispense;
    private Integer numberOfRefills;
    private String dosagePeriodUnit;
    private String uuid;
    private String practitionerName;
    private String practitionerId;
    private String createdAt;
    private String visitId;
    private String externalSystem;
    private String externalId;
    private String mrNumber;
    private String patientName;
}
