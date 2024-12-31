package com.serenity.integration.models;

import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class Practitioner {
  @JsonProperty("id")

  private String id; 

  @JsonProperty("uuid")

  private String uuid;
  
  @JsonProperty("country_code")

   private String countryCode;
   @JsonProperty("gender")

     private String gender;
    @JsonProperty("title")

    private String title;
    @JsonProperty("first_name")

    private String firstName;
    @JsonProperty("last_name")

    private String lastName;

    @JsonProperty("date_of_birth")
    private String dateOfBirth;
    private String email;
    private String mobile;
    private String postalAddress;
    private String homeAddress;
   
    
  
} 