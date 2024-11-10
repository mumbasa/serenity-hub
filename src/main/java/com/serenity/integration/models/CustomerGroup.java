package com.serenity.integration.models;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Setter
@Getter
@ToString
public class CustomerGroup {


    @JsonProperty("name")
    private String name;

    @JsonProperty("is_active")
    private boolean isActive;

    @JsonProperty("managing_organization")
    private String managingOrganization;

    @JsonProperty("uuid")
    private String uuid;

    @JsonProperty("created_by_id")
    private String createdById;

    @JsonProperty("created_by_name")
    private String createdByName;

}
