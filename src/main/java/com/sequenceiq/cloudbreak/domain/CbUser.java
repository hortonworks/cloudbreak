package com.sequenceiq.cloudbreak.domain;

import java.util.List;

public class CbUser {

    private String username;
    private String account;
    private List<CbUserRole> roles;

    public CbUser(String username, String account, List<CbUserRole> roles) {
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

    public List<CbUserRole> getRoles() {
        return roles;
    }

}
