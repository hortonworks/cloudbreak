package com.sequenceiq.cloudbreak.service.cluster;

import static com.sequenceiq.cloudbreak.common.network.NetworkConstants.SUBNET_ID;
import static com.sequenceiq.cloudbreak.common.network.NetworkConstants.SUBNET_IDS;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.domain.Network;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;

@Service
public class InstanceGroupSubnetCollector {

    private static final Logger LOGGER = LoggerFactory.getLogger(InstanceGroupSubnetCollector.class);

    public Set<String> collect(InstanceGroup instanceGroup, Network network) {
        String stackSubnetId = getStackSubnetIdIfExists(network);
        Set<String> instanceGroupSubnetIds = new HashSet<>(getOrDefault(getParameters(instanceGroup), SUBNET_IDS));
        if (StringUtils.isNotBlank(stackSubnetId) && instanceGroupSubnetIds.isEmpty()) {
            LOGGER.debug("Falling back to stack level network config and subnets: '{}'", stackSubnetId);
            return Set.of(stackSubnetId);
        }
        return instanceGroupSubnetIds;
    }

    private Map<String, Object> getParameters(InstanceGroup instanceGroup) {
        Json attributes = instanceGroup.getInstanceGroupNetwork() != null ? instanceGroup.getInstanceGroupNetwork().getAttributes() : null;
        return attributes == null ? Collections.emptyMap() : attributes.getMap();
    }

    private List<String> getOrDefault(Map<String, Object> params, String networkConstants) {
        return (List<String>) params.getOrDefault(networkConstants, new ArrayList<>());
    }

    private String getStackSubnetIdIfExists(Network network) {
        return Optional.ofNullable(network)
                .map(Network::getAttributes)
                .map(Json::getMap)
                .map(attr -> attr.get(SUBNET_ID))
                .map(Object::toString)
                .orElse(null);
    }
}
