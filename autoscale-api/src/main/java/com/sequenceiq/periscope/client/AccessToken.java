package com.sequenceiq.periscope.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AccessToken {

    private String token;

    private String tokenType;

    private int expiresIn;

    public AccessToken(@JsonProperty("access_token") String token, @JsonProperty("token_type") String tokenType, @JsonProperty("expires_in") int expiresIn) {
        this.token = token;
        this.tokenType = tokenType;
        this.expiresIn = expiresIn;
    }

    public String getToken() {
        return token;
    }

    public String getTokenType() {
        return tokenType;
    }

    public int getExpiresIn() {
        return expiresIn;
    }
}
