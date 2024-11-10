package com.serenity.integration.models;


import com.google.gson.annotations.SerializedName;


public class User {
@SerializedName("email")
private String email;
public String getEmail() {
    return email;
}
public void setEmail(String email) {
    this.email = email;
}
public void setPassword(String password) {
    this.password = password;
}
public String getPassword() {
    return password;
}
@SerializedName("password")
private String password;
}
