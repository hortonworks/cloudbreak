package com.sequenceiq.cloudbreak.structuredevent.event.cdp.environment;

import java.util.Map;
import java.util.Set;

import com.sequenceiq.cloudbreak.structuredevent.event.cdp.environment.proxy.ProxyDetails;
import com.sequenceiq.common.api.type.Tunnel;
import com.sequenceiq.environment.environment.domain.Region;
import com.sequenceiq.environment.environment.dto.FreeIpaCreationDto;
import com.sequenceiq.environment.environment.dto.telemetry.EnvironmentFeatures;
import com.sequenceiq.environment.network.dto.NetworkDto;
import com.sequenceiq.environment.parameter.dto.ParametersDto;

public interface EnvironmentDetails {

    String getName();

    Set<Region> getRegions();

    String getCloudPlatform();

    String getStatusReason();

    NetworkDto getNetwork();

    ParametersDto getParameters();

    Tunnel getTunnel();

    ProxyDetails getProxyDetails();

    EnvironmentFeatures getEnvironmentTelemetryFeatures();

    FreeIpaCreationDto getFreeIpaCreation();

    String getSecurityAccessType();

    Map<String, String> getUserDefinedTags();
}
