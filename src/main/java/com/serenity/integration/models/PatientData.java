package com.serenity.integration.models;

import org.apache.commons.csv.CSVRecord;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.gson.annotations.SerializedName;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.ToString;

@ToString

@Entity
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown=true)
@Table(name = "patient_information")
public class PatientData {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private long id;


  private String  uuid;
  
  @Column(nullable = true)
  @SerializedName("external_id")
  private String externalId;

  @SerializedName("external_system")
  private String externalSystem;
  @Column(nullable = true)

  @SerializedName("created_at")
  private String createdAt;
  @Column(columnDefinition = "TEXT")
  private String mobile;
  @Column(nullable = true)
  @SerializedName("national_mobile_number")

  private String nationalMobileNumber;
  @SerializedName("first_name")
  private String firstName;
  @SerializedName("last_name")

  private String lastName;
  @SerializedName("full_name")

  private String fullName;

  @Column(nullable = true)
  private String title;
  @Column(nullable = true)

  private String occupation;
  @Column(nullable = true)
  private String employer;
  @Column(nullable = true)

  private String email;
  @SerializedName("birth_date")

  private String birthDate;
  @Column(nullable = true)
  @SerializedName("marital_status")

  private String maritalStatus;

  private String gender;
  @Column(nullable = true)

  private String nationality;

  @Column(nullable = true)

  private String otherNames;

  @SerializedName("mr_number")
  private String mrNumber;

  @Column(nullable = true)
  @SerializedName("blood_type")

  private String bloodType;
  @Column(nullable = true)
  @SerializedName("passport_number")

  private String passportNumber;

  @Column(nullable = true)

  private String birthTime;
  @SerializedName("religious_affiliation")

  private String religiousAffiliation;
  @Column(name = "managing_organization_id")
  @SerializedName("managing_organization_id")
  private String managingOrganizationId;

  @Column(name = "managing_organization")
  @SerializedName("managing_organization")
  private String managingOrganization;

  public PatientData() {

  }

  public PatientData(CSVRecord record) {

    mrNumber = record.get("patient_id");
    lastName = record.get("plastname");
    firstName = record.get("pfirstname");
    mobile = record.get("mobile").replaceAll("-", "");
    email = record.get("email").isBlank()?null:record.get("email");
    birthDate = record.get("dob");
    // nationalId=record.get("countryid");
    gender = record.get("gender");
    externalSystem = "his";
    nationalMobileNumber = record.get("phone");
    fullName = record.get("pname");
    title = record.get("title");
    occupation = record.get("occupation");
    employer = record.get("employer");
    bloodType = record.get("bloodgroup");
    maritalStatus = record.get("maritalstatus");
    nationality = record.get("country");
    passportNumber = record.get("passport_no");
    // active=Boolean.parseBoolean(record.get("active"));
    birthTime = record.get("timeofbirth");
    religiousAffiliation = record.get("religiousaffiliation");
    // managingOrganizationId=record.get("membership");

  }

  public long getId() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
  }

  public String getUuid() {
    return uuid;
  }

  public void setUuid(String uuid) {
    this.uuid = uuid;
  }

  public String getExternalId() {
    return externalId;
  }

  public void setExternalId(String externalId) {
    this.externalId = externalId;
  }

  public String getExternalSystem() {
    return externalSystem;
  }

  public void setExternalSystem(String externalSystem) {
    this.externalSystem = externalSystem;
  }

  public String getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(String createdAt) {
    this.createdAt = createdAt;
  }

  public String getMobile() {
    return mobile;
  }

  public void setMobile(String mobile) {
    this.mobile = mobile;
  }

  public String getNationalMobileNumber() {
    return nationalMobileNumber;
  }

  public void setNationalMobileNumber(String nationalMobileNumber) {
    this.nationalMobileNumber = nationalMobileNumber;
  }

  public String getFirstName() {
    return firstName;
  }

  public void setFirstName(String firstName) {
    this.firstName = firstName;
  }

  public String getLastName() {
    return lastName;
  }

  public void setLastName(String lastName) {
    this.lastName = lastName;
  }

  public String getFullName() {
    return fullName;
  }

  public void setFullName(String fullName) {
    this.fullName = fullName;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getOccupation() {
    return occupation;
  }

  public void setOccupation(String occupation) {
    this.occupation = occupation;
  }

  public String getEmployer() {
    return employer;
  }

  public void setEmployer(String employer) {
    this.employer = employer;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getBirthDate() {
    return birthDate;
  }

  public void setBirthDate(String birthDate) {
    this.birthDate = birthDate;
  }

  public String getMaritalStatus() {
    return maritalStatus;
  }

  public void setMaritalStatus(String maritalStatus) {
    this.maritalStatus = maritalStatus;
  }

  public String getGender() {
    return gender.toUpperCase();
  }

  public void setGender(String gender) {
    this.gender = gender;
  }

  public String getNationality() {
    return nationality;
  }

  public void setNationality(String nationality) {
    this.nationality = nationality;
  }

  public String getOtherNames() {
    return otherNames;
  }

  public void setOtherNames(String otherNames) {
    this.otherNames = otherNames;
  }

  public String getMrNumber() {
    return mrNumber;
  }

  public void setMrNumber(String mrNumber) {
    this.mrNumber = mrNumber;
  }

  public String getBloodType() {
    return bloodType;
  }

  public void setBloodType(String bloodType) {
    this.bloodType = bloodType;
  }

  public String getPassportNumber() {
    return passportNumber;
  }

  public void setPassportNumber(String passportNumber) {
    this.passportNumber = passportNumber;
  }

  public String getBirthTime() {
    return birthTime;
  }

  public void setBirthTime(String birthTime) {
    this.birthTime = birthTime;
  }

  public String getReligiousAffiliation() {
    return religiousAffiliation;
  }

  public void setReligiousAffiliation(String religiousAffiliation) {
    this.religiousAffiliation = religiousAffiliation;
  }

  public String getManagingOrganizationId() {
    return managingOrganizationId;
  }

  public void setManagingOrganizationId(String managingOrganizationId) {
    this.managingOrganizationId = managingOrganizationId;
  }

public void setManagingOrganization(String string) {
    // TODO Auto-generated method stub
}

}
