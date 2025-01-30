package com.serenity.integration.models;

import java.time.Instant;
import java.util.UUID;

import lombok.Getter;
import lombok.Setter;
@Setter
@Getter
public class Diagnosis {
    private String condition;
    private String role;
    private Integer rank;
    private String code;
    private String system;
    private String status;
    private String note;
    private String practitionerName;
    private String uuid;
    private String patientId;
    private String encounterId;
    private String visitId;
    private String practitionerId;
    private String createdAt;

}
