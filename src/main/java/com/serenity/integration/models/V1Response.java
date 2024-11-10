package com.serenity.integration.models;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Setter
@Getter
@ToString
public class V1Response {
    private String email;
    private String password;
    private String refresh;
    private String access;
}
