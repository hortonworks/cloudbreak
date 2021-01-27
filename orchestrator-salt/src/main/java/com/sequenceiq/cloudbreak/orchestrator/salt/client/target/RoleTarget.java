package com.sequenceiq.cloudbreak.orchestrator.salt.client.target;

public class RoleTarget extends GrainTarget {

    private static final String ROLE_PREFIX = "roles:";

    public RoleTarget(String target) {
        super(ROLE_PREFIX + target);
    }

    @Override
    public String toString() {
        return "RoleTarget{" +
                "targets=" + getTarget() +
                '}';
    }
}
