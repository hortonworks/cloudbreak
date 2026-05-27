package com.sequenceiq.cloudbreak.service.database;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.exception.BadRequestException;

@Component
public class DatabaseProvisioningValidator {

    @Inject
    private DbOverrideConfig dbOverrideConfig;

    public void validateForProvisioning(String engineVersion, String runtime) {
        if (!dbOverrideConfig.isVersionSupportedForRuntime(engineVersion, runtime)) {
            String suggested = dbOverrideConfig.findEngineVersionForRuntime(runtime);
            String suggestion = suggested != null
                    ? String.format("Please use PostgreSQL %s or later.", suggested)
                    : "Please use a supported version.";
            throw new BadRequestException(String.format(
                    "Database engine version %s is not supported for runtime %s. %s", engineVersion, runtime, suggestion));
        }
    }
}
