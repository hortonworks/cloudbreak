package com.sequenceiq.cloudbreak.domain;

import java.util.List;

public class CbUser {

    private String username;
    private String account;
    private List<String> roles;

    public CbUser(String username, String account, List<String> roles) {
        this.username = username;
        this.account = account;
        this.roles = roles;
    }

    public String getUsername() {
        return username;
    }

    public String getAccount() {
        return account;
    }

    public List<String> getRoles() {
        return roles;
    }

}
