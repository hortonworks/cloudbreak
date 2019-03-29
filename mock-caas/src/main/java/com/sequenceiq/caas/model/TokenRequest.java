package com.sequenceiq.caas.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TokenRequest {

    @JsonProperty("code")
    private String authorizationCode;

    @JsonProperty("refresh_token")
    private String refreshToken;

    public TokenRequest() {
    }

    public TokenRequest(String refreshToken, String authorizationCode) {
        this.refreshToken = refreshToken;
        this.authorizationCode = authorizationCode;
    }

    public String getAuthorizationCode() {
        return authorizationCode;
    }

    public void setAuthorizationCode(String authorizationCode) {
        this.authorizationCode = authorizationCode;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }
}
