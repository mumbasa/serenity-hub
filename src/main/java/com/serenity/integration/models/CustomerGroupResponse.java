package com.serenity.integration.models;
import java.util.*;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
@Setter
@Getter
@ToString
public class CustomerGroupResponse {
List <CustomerGroup> data;
int total;
}
