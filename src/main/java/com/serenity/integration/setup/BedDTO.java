package com.serenity.integration.setup;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;

import lombok.Getter;
import lombok.Setter;
@Setter
@Getter
public class BedDTO {

   @SerializedName("room_uuid")
    private String roomUuid;

    @JsonProperty("name")
    private String name;

    public BedDTO (Bed bed){
        name =bed.getName();
        roomUuid=bed.getRoomUuid();

    }

    public BedDTO(){};
}
