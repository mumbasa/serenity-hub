package com.serenity.integration.setup;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Setter
@Getter
@ToString
public class Bed {
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

    @SerializedName("room_name")
    private String roomName;

    @SerializedName("name")
    private String name;

    @SerializedName("is_available")
    private boolean isAvailable;

    @SerializedName("room")
    private int room;

    @SerializedName("room_uuid")
    private String roomUuid;

}
