package com.sequenceiq.cloudbreak.orchestrator.salt.client

enum class SaltEndpoint private constructor(val contextPath: String) {

    BOOT_HEALTH("saltboot/health"),
    BOOT_PILLAR_SAVE("saltboot/salt/server/pillar"),
    BOOT_FILE_UPLOAD("saltboot/file"),
    BOOT_ACTION_DISTRIBUTE("saltboot/salt/action/distribute"),
    SALT_RUN("saltapi/run")
}
