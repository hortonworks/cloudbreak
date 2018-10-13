package com.sequenceiq.periscope.domain;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "periscope_user")
public class PeriscopeUser {

    @Id
    private String id;

    private String email;

    private String tenant;

    public PeriscopeUser() {
    }

    public PeriscopeUser(String id, String email, String tenant) {
        this.id = id;
        this.email = email;
        this.tenant = tenant;
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

    public String getTenant() {
        return tenant;
    }

    public void setTenant(String tenant) {
        this.tenant = tenant;
    }
}
