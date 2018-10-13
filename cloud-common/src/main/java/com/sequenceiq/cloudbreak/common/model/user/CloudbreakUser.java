package com.sequenceiq.cloudbreak.common.model.user;

import java.util.Objects;

public class CloudbreakUser {

    private final String userId;

    private final String username;

    private final String tenant;

    public CloudbreakUser(String userId, String username, String tenant) {
        this.userId = userId;
        this.username = username;
        this.tenant = tenant;
    }

    public String getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public String getTenant() {
        return tenant;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (o == null || !getClass().equals(o.getClass())) {
            return false;
        }
        CloudbreakUser iu = (CloudbreakUser) o;
        return Objects.equals(userId, iu.userId) && Objects.equals(tenant, iu.tenant);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, tenant);
    }
}
