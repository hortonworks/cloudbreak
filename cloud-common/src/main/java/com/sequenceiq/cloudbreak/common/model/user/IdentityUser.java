package com.sequenceiq.cloudbreak.common.model.user;

import java.util.Collection;
import java.util.Date;
import java.util.List;

public class IdentityUser {

    private final String userId;

    private final String username;

    private final String account;

    private final List<IdentityUserRole> roles;

    private final String givenName;

    private final String familyName;

    private final Date created;

    public IdentityUser(String userId, String username, String account, List<IdentityUserRole> roles, String givenName, String familyName, Date created) {
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

    public Collection<IdentityUserRole> getRoles() {
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
