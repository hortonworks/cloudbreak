package com.sequenceiq.cloudbreak.model;

import io.swagger.annotations.ApiModel;

@ApiModel("User")
public class UserRequest {

    private String username;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
