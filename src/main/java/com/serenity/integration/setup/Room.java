package com.serenity.integration.setup;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Setter
@Getter
@ToString
public class Room {
 @SerializedName("id")
    private int id;

    @SerializedName("uuid")
    private String uuid;

    @SerializedName("created_at")
    private String createdAt;

    @SerializedName("is_deleted")
    private boolean isDeleted;

    @SerializedName("modified_at")
    private String modifiedAt;

    @JsonProperty("name")
    private String name;

    @SerializedName("ward_name")
    private String wardName;

    @SerializedName("ward_uuid")
    private String wardUuid;


    @SerializedName("total_beds")
    private String totalBeds;

    @SerializedName("available_beds")
    private String availableBeds; // Nullable field

    @SerializedName("ward")
    private String ward;

}
