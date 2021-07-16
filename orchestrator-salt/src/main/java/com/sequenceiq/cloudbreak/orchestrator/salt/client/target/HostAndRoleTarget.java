package com.sequenceiq.cloudbreak.orchestrator.salt.client.target;

import java.util.Collection;
import java.util.Objects;

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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (o == null || getClass() != o.getClass()) {
            return false;
        } else {
            HostAndRoleTarget that = (HostAndRoleTarget) o;
            return Objects.equals(role, that.role) && (Objects.equals(hosts, that.hosts) || collectionsContainSameElements(hosts, that.hosts));
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(role, hosts);
    }

    private boolean collectionsContainSameElements(Collection<String> hosts, Collection<String> hosts1) {
        return hosts != null && hosts1 != null && hosts.size() == hosts1.size() && hosts.containsAll(hosts1);
    }
}
