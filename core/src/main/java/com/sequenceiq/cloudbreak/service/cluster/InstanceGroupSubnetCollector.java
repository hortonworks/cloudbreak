package com.sequenceiq.cloudbreak.service.cluster;

import static com.sequenceiq.cloudbreak.common.network.NetworkConstants.SUBNET_ID;
import static com.sequenceiq.cloudbreak.common.network.NetworkConstants.SUBNET_IDS;
import static java.util.stream.Collectors.toSet;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.domain.Network;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;

@Service
public class InstanceGroupSubnetCollector {

    public Set<String> collect(InstanceGroup instanceGroup, Network network) {
        String stackSubnetId = getStackSubnetIdIfExists(network);
        Set<String> instanceGroupSubnetIds = getOrDefault(getParameters(instanceGroup), SUBNET_IDS)
                .stream()
                .collect(toSet());
        if (StringUtils.isNotBlank(stackSubnetId) && (instanceGroupSubnetIds == null || instanceGroupSubnetIds.isEmpty())) {
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
