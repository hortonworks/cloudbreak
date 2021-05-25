package com.sequenceiq.cloudbreak.orchestrator.salt.client.target;

import java.util.Collection;

public class HostAndRoleTarget implements Target<String> {

    private static final String ROLE_PREFIX = "G@roles:";

    private static final String COMPOUND_AND = " and ";

    private static final String HOST_PREFIX = "L@";

    private String role;

    private Collection<String> hosts;

    public HostAndRoleTarget(String role, Collection<String> hosts) {
        this.role = role;
        this.hosts = hosts;
    }

    @Override
    public String getTarget() {
        return ROLE_PREFIX + role + COMPOUND_AND + HOST_PREFIX + String.join(",", hosts);
    }

    @Override
    public String getType() {
        return "compound";
    }

        @Override
    public String toString() {
        return "HostAndRoleTarget{" +
                "role=" + role +
                ",hosts=" + hosts +
                '}';
    }
}
