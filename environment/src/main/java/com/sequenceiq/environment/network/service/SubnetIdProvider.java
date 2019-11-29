package com.sequenceiq.environment.network.service;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.environment.network.dao.domain.RegistrationType;
import com.sequenceiq.environment.network.dto.NetworkDto;

@Component
public class SubnetIdProvider {

    public String provide(NetworkDto network) {
        return RegistrationType.EXISTING == network.getRegistrationType() ? network.getSubnetIds().iterator().next()
                : getPublicSubnetIds(network.getSubnetMetas()).iterator().next();
    }

    private Set<String> getPublicSubnetIds(Map<String, CloudSubnet> subnetMetas) {
        return subnetMetas.values().stream()
                .filter(cloudSubnet -> !cloudSubnet.isPrivateSubnet())
                .map(CloudSubnet::getId)
                .collect(Collectors.toSet());
    }
}
