package com.serenity.integration.models;

import java.time.Instant;
import java.util.UUID;

import jakarta.annotation.Generated;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
@Setter
@Entity
@Table
@Getter
public class Diagnosis {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;
    @Column(columnDefinition="TEXT")

    private String condition;
    private String role;
    private Integer rank;
    private String code;
    private String system;
    private String status;
    @Column(columnDefinition = "TEXT")
    private String note;
    private String practitionerName;
    private String uuid;
    private String patientId;
    private String encounterId;
    private String visitId;
    private String practitionerId;
    private String createdAt;
    //private String externalSystem;
    //private String externalId;

}
