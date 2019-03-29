package com.sequenceiq.caas.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CaasUserList {

    @JsonProperty("count")
    private int count;

    @JsonProperty("users")
    private List<CaasUser> users;

    public CaasUserList(List<CaasUser> users) {
        this.users = users;
        count = users.size();
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public List<CaasUser> getUsers() {
        return users;
    }

    public void setUsers(List<CaasUser> users) {
        this.users = users;
    }
}
