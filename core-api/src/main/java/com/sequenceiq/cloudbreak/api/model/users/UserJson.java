package com.sequenceiq.cloudbreak.api.model.users;

import io.swagger.annotations.ApiModel;

@ApiModel
public class UserJson {

    private String username;

    public UserJson() {
    }

    public UserJson(String username) {
        this.username = username;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
