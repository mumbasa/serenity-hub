package com.serenity.integration.serenity;

import java.time.ZonedDateTime;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class Admission {
 private String uuid;
    private ZonedDateTime createdAt;
    private ZonedDateTime updatedAt;
    private String patientId;
    private String healthcareServiceName;
    private String admittedByName;
    private String createdByName;
    private String admitSource;
    private String admitSourceDisplay;
    private ZonedDateTime admittedAt;
    private String origin;
    private String priceTierName;
    private boolean isActive;
    private boolean isReadmission;
    private String roomName;
    private String bedName;
    private String reason;
    private String transferredByName;
    private ZonedDateTime transferredAt;
    private String locationId;
    private String locationName;
    private String status;
    private String admittedBy;
    private String createdBy;
    private String healthcareServiceId;
    private String priceTierId;
    private double charge;
    private String currency;
    private String providerId;
    private String bedId;
    private String roomId;
    private String visitId;
    private String dischargeDisposition;
    private String dischargedById;
    private ZonedDateTime dischargedAt;
    private String dischargedByName;
    private String transferredById;
    private boolean isDeleted;
    private int totalBilledCycles;
    private ZonedDateTime lastBillingDatetime;
    private String notes;
    private String patientFullName;
    private String patientMobile;
    private String patientGender;
    private ZonedDateTime patientBirthDate;
    private String patientMrNumber;
    private ZonedDateTime dischargeAuthorizedAt;
    private String dischargeAuthorizedByName;
    private String dischargeAuthorizedById;
    private String destinationDisplay;
}
