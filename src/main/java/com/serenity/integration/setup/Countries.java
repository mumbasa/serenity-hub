package com.serenity.integration.setup;

import java.util.List;

import com.google.gson.annotations.SerializedName;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class Countries {
    @SerializedName("choices")
List<String> choices;
}
