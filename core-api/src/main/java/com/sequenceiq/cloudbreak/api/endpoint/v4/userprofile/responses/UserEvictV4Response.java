package com.sequenceiq.cloudbreak.api.endpoint.v4.userprofile.responses;

import io.swagger.annotations.ApiModel;

@ApiModel
public class UserEvictV4Response {

    private String username;

    public UserEvictV4Response() {
    }

    public UserEvictV4Response(String username) {
        this.username = username;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
