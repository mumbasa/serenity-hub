package com.serenity.integration.models;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Table
@Entity
@Setter
@Getter
public class PatientMapping {
@Id
@GeneratedValue(strategy=GenerationType.SEQUENCE)
private long id;
private String source;
private String sourceId;
private String serenityId;
}
