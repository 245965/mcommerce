package com.example.myapplication.model;

public class RegisterResponse {
    private String message;
    private String avatarUrl;
    private String firstName;
    private String lastName;
    private String token;

    public String getMessage()   { return message; }
    public String getAvatarUrl() { return avatarUrl; }
    public String getFirstName() { return firstName; }
    public String getLastName()  { return lastName; }
    public String getToken()     { return token; }
}

