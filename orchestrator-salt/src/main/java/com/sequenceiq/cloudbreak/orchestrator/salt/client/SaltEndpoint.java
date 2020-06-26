package com.sequenceiq.cloudbreak.orchestrator.salt.client;

public enum SaltEndpoint {

    BOOT_HEALTH("saltboot/health"),
    BOOT_PILLAR_SAVE("saltboot/salt/server/pillar"),
    BOOT_PILLAR_DISTRIBUTE("saltboot/salt/server/pillar/distribute"),
    BOOT_FILE_UPLOAD("saltboot/file"),
    BOOT_FILE_DISTRIBUTE("saltboot/file/distribute"),
    BOOT_ACTION_DISTRIBUTE("saltboot/salt/action/distribute"),
    BOOT_HOSTNAME_ENDPOINT("saltboot/hostname/distribute"),
    BOOT_FINGERPRINT_DISTRIBUTE("saltboot/salt/minion/fingerprint/distribute"),
    SALT_RUN("saltapi/run");

    private final String contextPath;

    SaltEndpoint(String contextPath) {
        this.contextPath = contextPath;
    }

    public String getContextPath() {
        return contextPath;
    }
}
