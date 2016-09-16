package com.sequenceiq.cloudbreak.orchestrator.salt.client;

public enum SaltEndpoint {

    BOOT_HEALTH("saltboot/health"),
    BOOT_PILLAR_SAVE("saltboot/salt/server/pillar"),
    BOOT_FILE_UPLOAD("saltboot/file"),
    BOOT_ACTION_DISTRIBUTE("saltboot/salt/action/distribute"),
    BOOT_HOSTNAME_ENDPOINT("saltboot/hostname/distribute"),
    SALT_RUN("saltapi/run");

    private String contextPath;

    SaltEndpoint(String contextPath) {
        this.contextPath = contextPath;
    }

    public String getContextPath() {
        return contextPath;
    }
}
