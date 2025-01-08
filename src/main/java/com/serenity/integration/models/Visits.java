package com.serenity.integration.models;

import java.beans.Transient;
import java.time.ZonedDateTime;
import java.util.UUID;

import jakarta.annotation.Generated;
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
public class Visits {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;
    private UUID uuid;
    private String externalId;
    private String externalSystem;
    private String createdAt;
    private String status;
    private String userFriendlyId;
    private String encounterClass;
    private String display;
    private String priority;
    private String startedAt;
    private String endedAt;
    private String locationId;
    private String locationName;
    private String assignedToName;
    private String assignedToId;
    private String serviceProviderId;
    private String patientId;
    private String serviceProviderName;
    private String patientName;
    private String patientMobile;
    private String patientMrNumber;
    private String patientDob;
    private String gender;
    private String hisNumber;
    private String patientStatus;
    private String practitionerId;


    @jakarta.persistence.Transient 
    private Doctors doctors;

}
