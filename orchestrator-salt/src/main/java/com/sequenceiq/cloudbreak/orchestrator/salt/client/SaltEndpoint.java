package com.sequenceiq.cloudbreak.orchestrator.salt.client;

public enum SaltEndpoint {

    BOOT_HEALTH("cbboot/health"),
    BOOT_PILLAR_SAVE("cbboot/salt/server/pillar"),
    BOOT_ACTION_DISTRIBUTE("cbboot/salt/action/distribute"),
    SALT_RUN("saltapi/run");

    private String contextPath;

    SaltEndpoint(String contextPath) {
        this.contextPath = contextPath;
    }

    public String getContextPath() {
        return contextPath;
    }
}
