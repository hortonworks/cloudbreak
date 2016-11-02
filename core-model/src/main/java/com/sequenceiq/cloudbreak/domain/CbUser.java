package com.sequenceiq.cloudbreak.domain;

import java.util.Date;
import java.util.List;

import com.sequenceiq.cloudbreak.common.type.CbUserRole;

public class CbUser {

    private final String userId;

    private final String username;

    private final String account;

    private final List<CbUserRole> roles;

    private final String givenName;

    private final String familyName;

    private final Date created;

    public CbUser(String userId, String username, String account, List<CbUserRole> roles, String givenName, String familyName, Date created) {
        this.userId = userId;
        this.username = username;
        this.account = account;
        this.roles = roles;
        this.givenName = givenName;
        this.familyName = familyName;
        this.created = created;
    }

    public String getUserId() {
        return userId;
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

    public Date getCreated() {
        return created;
    }
}
