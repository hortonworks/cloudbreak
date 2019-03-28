package com.sequenceiq.cloudbreak.api.endpoint.v4.workspace.responses;

import com.sequenceiq.cloudbreak.api.endpoint.v4.JsonEntity;

import io.swagger.annotations.ApiModel;

@ApiModel
public class UserV4Response implements JsonEntity {

    private long id;

    private String username;

    private String userId;

    public UserV4Response() {
    }

    public UserV4Response(long id, String username, String userId) {
        this.id = id;
        this.username = username;
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }
}
