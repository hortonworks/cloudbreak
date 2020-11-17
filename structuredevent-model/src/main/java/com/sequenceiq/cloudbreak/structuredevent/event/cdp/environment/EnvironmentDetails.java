package com.sequenceiq.cloudbreak.structuredevent.event.cdp.environment;

import java.util.Set;

import com.sequenceiq.environment.environment.domain.Region;
import com.sequenceiq.environment.network.dto.NetworkDto;

public interface EnvironmentDetails {
    String getName();

    void setName(String name);

    Set<Region> getRegions();

    void setRegions(Set<Region> regions);

    String getCloudPlatform();

    void setCloudPlatform(String cloudPlatform);

    String getStatusReason();

    void setStatusReason(String statusReason);

    NetworkDto getNetwork();

    void setNetwork(NetworkDto network);
}
