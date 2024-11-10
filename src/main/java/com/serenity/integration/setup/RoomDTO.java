package com.serenity.integration.setup;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class RoomDTO {

    @JsonProperty("name")
    private String name;

     @SerializedName("ward_uuid")
    private String wardUuid;


    public RoomDTO(Room room){
        name=room.getName();
        wardUuid=room.getWardUuid();

    }
}
