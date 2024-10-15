package com.serenity.integration.models;

import java.time.LocalDateTime;

import org.apache.commons.csv.CSVRecord;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
@ToString
@Setter
@Getter
@Entity
@Table(name="patient_information")
public class PatientData {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;
    private String uuid;
    @Column(nullable = true)

    private String externalId;
    
    private String externalSystem;
    @Column(nullable = true)

    private LocalDateTime createdAt;
    
    private String mobile;
    @Column(nullable = true)

    private String nationalMobileNumber;
    
    private String firstName;
    
    private String lastName;

    private String fullName;
    
@Column(nullable = true)
    private String title;
    @Column(nullable = true)

    private String occupation;
    @Column(nullable = true)
    private String employer;
    @Column(nullable = true)

    private String email;
    
    private String birthDate;
    @Column(nullable = true)

    private String maritalStatus;
    
    private String gender;
    @Column(nullable = true)

    private String nationality;
    
    
    @Column(nullable = true)

    private String otherNames;
    
    private String mrNumber;
    
    @Column(nullable = true)
     
  
    private String bloodType;
    @Column(nullable = true)

    private String passportNumber;

    @Column(nullable = true)

    private String birthTime;

    private String religiousAffiliation;


    public PatientData(){


    } 



    public PatientData(CSVRecord record){

        mrNumber=record.get("patient_id");
        lastName=record.get("plastname");
        firstName=record.get("pfirstname");
        mobile=record.get("mobile").replaceAll("-", "");
        email=record.get("email");
        birthDate=record.get("dob");
      //  nationalId=record.get("countryid");
        gender=record.get("gender");
        externalSystem="his";
        nationalMobileNumber=record.get("phone");
        fullName=record.get("pname");
        title=record.get("title");
        occupation=record.get("occupation");
        employer=record.get("employer");
        bloodType=record.get("bloodgroup");
        maritalStatus=record.get("maritalstatus");
        nationality=  record.get("country");
        passportNumber=record.get("passport_no");
      //  active=Boolean.parseBoolean(record.get("active"));
        birthTime=record.get("timeofbirth");
        religiousAffiliation=record.get("religiousaffiliation");
       // managingOrganizationId=record.get("membership");

    } 

    
}
