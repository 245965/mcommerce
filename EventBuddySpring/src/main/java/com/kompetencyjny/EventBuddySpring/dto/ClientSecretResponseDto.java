package com.kompetencyjny.EventBuddySpring.dto;

public class ClientSecretResponseDto {
    private String clientSecret;
    private String error;

    public ClientSecretResponseDto() {}

    public ClientSecretResponseDto(String clientSecret, String error) {
        this.clientSecret = clientSecret;
        this.error = error;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }
}
