package com.sequenceiq.freeipa.service.filter;

import java.util.Collection;
import java.util.Map;

import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.freeipa.entity.Network;

public interface NetworkFilterProvider {

    Map<String, String> provide(Network network, String networkId, Collection<String> subnetIds);

    CloudPlatform cloudPlatform();
}
