package com.sequenceiq.cloudbreak.converter.v4.environment.network;

import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.network.CloudSubnet;
import com.sequenceiq.cloudbreak.domain.environment.Subnet;
import com.sequenceiq.cloudbreak.domain.environment.SubnetVisibility;

@Component
class SubnetConverter {

    Set<Subnet> convert(Set<CloudSubnet> cloudSubnets) {
        return cloudSubnets.stream()
                .map(this::convertSubnet)
                .collect(Collectors.toSet());
    }

    private Subnet convertSubnet(CloudSubnet cloudSubnet) {
        Subnet subnet = new Subnet();
        subnet.setSubnetId(cloudSubnet.getSubnetId());
        subnet.setCidr(cloudSubnet.getCidr());
        subnet.setAvailabilityZone(cloudSubnet.getAvailabilityZone());
        subnet.setVisibility(cloudSubnet.isPrivateSubnet() ? SubnetVisibility.PRIVATE : SubnetVisibility.PUBLIC);
        return subnet;
    }
}
