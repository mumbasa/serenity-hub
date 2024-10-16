package com.serenity.integration.models;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDate;
import java.time.LocalDateTime;

import java.util.UUID;

@Setter
@Getter
@ToString
@Entity
@Table(name = "encounter")
public class Encounter {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;
   
    @Column(name = "uuid", nullable = false, unique = true)
    private String uuid;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "encounter_class")
    private String encounterClass;

    @Column(name = "status")
    private String status;

    @Column(name = "display")
    private String display;

    @Column(name = "priority")
    private String priority;

    @Column(name = "planned_start")
    private LocalDateTime plannedStart;

    @Column(name = "planned_end")
    private LocalDateTime plannedEnd;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    @Column(name = "external_id")
    private String externalId;

    @Column(name = "external_system")
    private String externalSystem;

    @Column(name = "appointment_id")
    private String appointmentId;

    @Column(name = "location_id")
    private String locationId;

    @Column(name = "location_name")
    private String locationName;

    @Column(name = "service_type_id")
    private String serviceTypeId;

    @Column(name = "service_type_name")
    private String serviceTypeName;

    @Column(name = "service_provider_id")
    private String serviceProviderId;

    @Column(name = "patient_mr_number")
    private String patientMrNumber;

    @Column(name = "patient_id")
    private String patientId;

    @Column(name = "patient_full_name")
    private String patientFullName;

    @Column(name = "patient_mobile")
    private String patientMobile;

    @Column(name = "patient_birth_date")
    private String patientBirthDate;

    @Column(name = "patient_gender")
    private String patientGender;

    @Column(name = "patient_status")
    private String patientStatus;

    @Column(name = "created_by_id")
    private String createdById;

    @Column(name = "created_by_name")
    private String createdByName;

    @Column(name = "user_friendly_id")
    private String userFriendlyId;

    @Column(name = "assigned_to_name")
    private String assignedToName;

    @Column(name = "assigned_to_id")
    private String assignedToId;

    @Column(name = "service_provider_name")
    private String serviceProviderName;

  
}
