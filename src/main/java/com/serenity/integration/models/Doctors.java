package com.serenity.integration.models;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Entity
@Setter
@Getter
@ToString
@Table(name = "doctors")
public class Doctors {
     @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private long id;

    @Column(name = "country_code")
    private String countryCode;

    private String gender;

    private String title;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Column(name = "date_of_birth")
    private String dateOfBirth;

    private String email;

    private String mobile;

    @Column(name = "postal_address")
    private String postalAddress;

    @Column(name = "home_address")
    private String homeAddress;
    
    private String fullName;
    private String hisId;
    private String empId;
    private String serenityId;
    private String serenityUUid;
    private String createdAt;
    private String externalId;
    private String externalSystem;
    private String managingOrganisation;
    private String managingOrganisationId;
    private String nationalMobileNumber;

}
