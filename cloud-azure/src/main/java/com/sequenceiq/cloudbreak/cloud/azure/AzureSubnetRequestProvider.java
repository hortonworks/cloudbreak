package com.sequenceiq.cloudbreak.cloud.azure;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.network.NetworkSubnetRequest;
import com.sequenceiq.cloudbreak.cloud.model.network.SubnetRequest;

@Component
public class AzureSubnetRequestProvider {

    public List<SubnetRequest> provide(String region, List<NetworkSubnetRequest> publicSubnets,
        List<NetworkSubnetRequest> privateSubnets, boolean privateSubnetEnabled) {
        List<SubnetRequest> subnets = new ArrayList<>();
        int index = 0;

        for (int i = 0; i < publicSubnets.size(); i++) {
            NetworkSubnetRequest networkSubnetRequest = publicSubnets.get(i);
            SubnetRequest subnetRequest = getSubnetRequest(region, networkSubnetRequest);
            subnetRequest.setSubnetGroup(i % publicSubnets.size());
            subnetRequest.setIndex(index++);
            subnets.add(subnetRequest);
        }

        if (privateSubnetEnabled) {
            for (int i = 0; i < privateSubnets.size(); i++) {
                NetworkSubnetRequest networkSubnetRequest = privateSubnets.get(i);
                SubnetRequest subnetRequest = getSubnetRequest(region, networkSubnetRequest);
                subnetRequest.setSubnetGroup(i % publicSubnets.size());
                subnetRequest.setIndex(index++);
                subnets.add(subnetRequest);
            }
        }

        return subnets;
    }

    private SubnetRequest getSubnetRequest(String region, NetworkSubnetRequest networkSubnetRequest) {
        SubnetRequest subnetRequest = new SubnetRequest();
        subnetRequest.setPublicSubnetCidr(networkSubnetRequest.getCidr());
        subnetRequest.setType(networkSubnetRequest.getType());
        subnetRequest.setAvailabilityZone(region);
        return subnetRequest;
    }
}
