package com.serenity.integration.setup;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class BedResponse {
private String message;
private List<Bed> results;
}
