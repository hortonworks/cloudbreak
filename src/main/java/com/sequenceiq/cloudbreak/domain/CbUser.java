package com.sequenceiq.cloudbreak.domain;

import java.util.List;

public class CbUser {

    private String username;
    private String account;
    private List<CbUserRole> roles;
    private String givenName;
    private String familyName;

    public CbUser(String username, String account, List<CbUserRole> roles, String givenName, String familyName) {
        this.username = username;
        this.account = account;
        this.roles = roles;
        this.givenName = givenName;
        this.familyName = familyName;
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

    public String getGivenName() {
        return givenName;
    }

    public String getFamilyName() {
        return familyName;
    }
}
