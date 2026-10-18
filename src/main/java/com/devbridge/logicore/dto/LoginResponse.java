package com.devbridge.logicore.dto;

public class LoginResponse {

    private final String accessToken;
    private final String tokenType = "Bearer";
    private final long expiresInSeconds;
    private final UserResponse user;

    public LoginResponse(String accessToken, long expiresInSeconds, UserResponse user) {
        this.accessToken = accessToken;
        this.expiresInSeconds = expiresInSeconds;
        this.user = user;
    }

    public String getAccessToken() { return accessToken; }
    public String getTokenType() { return tokenType; }
    public long getExpiresInSeconds() { return expiresInSeconds; }
    public UserResponse getUser() { return user; }
}