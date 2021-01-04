package com.sequenceiq.cloudbreak.structuredevent.event.cdp.environment;

import java.util.Set;

import com.sequenceiq.common.api.type.Tunnel;
import com.sequenceiq.environment.environment.domain.Region;
import com.sequenceiq.environment.environment.dto.telemetry.EnvironmentFeatures;
import com.sequenceiq.environment.network.dto.NetworkDto;

public interface EnvironmentDetails {

    String getName();

    Set<Region> getRegions();

    String getCloudPlatform();

    String getStatusReason();

    NetworkDto getNetwork();

    Tunnel getTunnel();

    EnvironmentFeatures getEnvironmentTelemetryFeatures();
}
