package com.sequenceiq.periscope.domain;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

@Entity
@Table(name = "periscope_user")
@NamedQuery(
        name = "PeriscopeUser.findOneByName",
        query = "SELECT b FROM PeriscopeUser b "
                + "WHERE b.email= :email")
public class PeriscopeUser {

    @Id
    private String id;

    private String email;

    private String account;

    public PeriscopeUser() {
    }

    public PeriscopeUser(String id, String email, String account) {
        this.id = id;
        this.email = email;
        this.account = account;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }
}
