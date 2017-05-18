package com.sequenceiq.cloudbreak.api.model;

import io.swagger.annotations.ApiModel;

@ApiModel("User")
public class User {

    private String username;

    public User() {
    }

    public User(String username) {
        this.username = username;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
