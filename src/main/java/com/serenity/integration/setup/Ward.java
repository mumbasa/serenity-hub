package com.serenity.integration.setup;

import java.util.List;

import com.google.gson.annotations.SerializedName;
import com.serenity.integration.models.PriceTier;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Setter
@Getter
@ToString
public class Ward {

    @SerializedName("name")
    private String wardName;

    @SerializedName("description")
    private String description;

    @SerializedName("locations")
    private List<Location> locations;

    @SerializedName("specialties")
    private List<String> specialties;

    @SerializedName("service_class")
    private String serviceClass;

    @SerializedName("subscription_frequency")
    private String subscriptionFrequency;

    @SerializedName("ward_type")
    private String wardType;

    @SerializedName("max_capacity")
    private int maxCapacity;

    @SerializedName("category")
    private String category;

    @SerializedName("section")
    private String section;

    @SerializedName("type")
    private String type;
    
    private String price;

    @SerializedName("price_tiers")
    private List<PriceTier> priceTiers;
}
