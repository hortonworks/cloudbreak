package com.sequenceiq.cloudbreak.structuredevent.service.telemetry.converter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cloudera.thunderhead.service.common.usage.UsageProto.CDPEnvironmentDeletionType;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentDeletionType;

public class EnvironmentDeletionTypeToCDPEnvironmentDeletionType {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentDeletionTypeToCDPEnvironmentDeletionType.class);

    private EnvironmentDeletionTypeToCDPEnvironmentDeletionType() {
    }

    public static CDPEnvironmentDeletionType.Value convert(String environmentDeletionTypeName) {
        if (environmentDeletionTypeName == null) {
            return CDPEnvironmentDeletionType.Value.UNSET;
        }
        try {
            EnvironmentDeletionType environmentDeletionType = EnvironmentDeletionType.valueOf(environmentDeletionTypeName);
            return switch (environmentDeletionType) {
                case NONE -> CDPEnvironmentDeletionType.Value.NONE;
                case SIMPLE -> CDPEnvironmentDeletionType.Value.SIMPLE;
                case FORCE -> CDPEnvironmentDeletionType.Value.FORCE;
            };
        } catch (IllegalArgumentException e) {
            LOGGER.warn("Cannot convert '{}' to CDPEnvironmentDeletionType! Returning default value '{}'.",
                    environmentDeletionTypeName, CDPEnvironmentDeletionType.Value.UNSET, e);
            return CDPEnvironmentDeletionType.Value.UNSET;
        }
    }
}
