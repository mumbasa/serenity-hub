package com.serenity.integration.models;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Entity
@Table

public class Visit {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;
    private String uuid;
    private String createdAt;
    private String updatedAt;
    private String encounterClass;
    private String status;
    private String display;
    private String priority;
    private String plannedStart;
    private String plannedEnd;
    private String startedAt;
    private String endedAt;
    private String externalId;
    private String externalSystem;
    private String appointmentId;
    private String locationId;
    private String locationName;
    private String serviceTypeId;
    private String serviceTypeName;
    private String serviceProviderId;
    private String patientMrNumber;
    private String patientId;
    private String patientFullName;
    private String patientMobile;
    private String patientBirthDate;
    private String patientGender;
    private String patientStatus;
    private String createdById;
    private String createdByName;
    private String userFriendlyId;
    private String assignedToName;
    private String assignedToId;
    private String serviceProviderName;
    private String slotId;

}
