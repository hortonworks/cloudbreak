package com.sequenceiq.cloudbreak.api.model.users;

import java.io.Serializable;
import java.util.Comparator;

import io.swagger.annotations.ApiModel;

@ApiModel
public class UserResponseJson {

    private long id;

    private String username;

    private String userId;

    public UserResponseJson() {
    }

    public UserResponseJson(long id, String username, String userId) {
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

    public static class UserIdComparator implements Comparator<UserResponseJson>, Serializable {
        @Override
        public int compare(UserResponseJson o1, UserResponseJson o2) {
            return o1.getUserId().compareTo(o2.getUserId());
        }
    }
}
