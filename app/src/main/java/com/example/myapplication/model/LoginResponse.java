package com.example.myapplication.model;

public class LoginResponse {
    private String token;
    private String email;
    private String firstName;
    private String lastName;
    private String avatarUrl;

    public String getToken()     { return token; }
    public String getEmail()     { return email; }
    public String getFirstName() { return firstName; }
    public String getLastName()  { return lastName; }
    public String getAvatarUrl() { return avatarUrl; }
}
