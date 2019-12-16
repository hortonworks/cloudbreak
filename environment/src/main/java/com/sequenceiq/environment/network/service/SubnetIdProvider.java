package com.sequenceiq.environment.network.service;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.common.api.type.Tunnel;
import com.sequenceiq.environment.network.dto.NetworkDto;

@Component
public class SubnetIdProvider {

    public String provide(NetworkDto network, Tunnel tunnel) {
        return getSubnetIdByPreferedSubnetType(network, tunnel.useCcm());
    }

    private String getSubnetIdByPreferedSubnetType(NetworkDto network, boolean preferPrivate) {
        Optional<CloudSubnet> subnet =
                network.getSubnetMetas().values().stream().filter(cloudSubnet -> preferPrivate == cloudSubnet.isPrivateSubnet()).findAny();
        return subnet.isPresent() ? subnet.get().getId() : network.getSubnetIds().iterator().next();
    }
}
