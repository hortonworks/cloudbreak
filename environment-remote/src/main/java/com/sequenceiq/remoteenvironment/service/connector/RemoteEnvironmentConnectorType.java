package com.sequenceiq.remoteenvironment.service.connector;

import java.util.Arrays;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor;

public enum RemoteEnvironmentConnectorType {

    PRIVATE_CONTROL_PLANE(CrnResourceDescriptor.ENVIRONMENT),
    CLASSIC_CLUSTER(CrnResourceDescriptor.CLASSIC_CLUSTER);

    private static final Logger LOGGER = LoggerFactory.getLogger(RemoteEnvironmentConnectorType.class);

    private final Set<CrnResourceDescriptor> crnResourceDescriptors;

    RemoteEnvironmentConnectorType(CrnResourceDescriptor... crnResourceDescriptors) {
        this.crnResourceDescriptors = Set.of(crnResourceDescriptors);
    }

    public static RemoteEnvironmentConnectorType getByCrn(String crn) {
        CrnResourceDescriptor crnResourceDescriptor = CrnResourceDescriptor.getByCrnString(crn);
        return Arrays.stream(values())
                .filter(platform -> platform.crnResourceDescriptors.contains(crnResourceDescriptor))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Based on CRN we were not able to decide the remote environment type"));
    }
}
