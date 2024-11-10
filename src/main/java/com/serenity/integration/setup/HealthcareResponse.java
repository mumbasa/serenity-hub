package com.serenity.integration.setup;
import java.util.*;

import com.serenity.integration.models.HealthcareService;

import lombok.Getter;
import lombok.Setter;
@Setter
@Getter
public class HealthcareResponse {
List <HealthcareService> data;
int total;
}
