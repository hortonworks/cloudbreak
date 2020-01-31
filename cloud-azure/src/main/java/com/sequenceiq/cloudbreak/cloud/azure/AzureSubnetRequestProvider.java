package com.sequenceiq.cloudbreak.cloud.azure;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.network.SubnetRequest;

@Component
public class AzureSubnetRequestProvider {

    public List<SubnetRequest> provide(String region, List<String> publicSubnetCidrs,  List<String> privateSubnetCidrs) {
        List<SubnetRequest> subnets = new ArrayList<>();
        int index = 0;

        for (int i = 0; i < publicSubnetCidrs.size(); i++) {
            SubnetRequest subnetRequest = new SubnetRequest();
            subnetRequest.setPublicSubnetCidr(publicSubnetCidrs.get(i));
            subnetRequest.setAvailabilityZone(region);
            subnetRequest.setSubnetGroup(i % publicSubnetCidrs.size());
            subnetRequest.setIndex(index++);
            subnets.add(subnetRequest);
        }

        for (int i = 0; i < privateSubnetCidrs.size(); i++) {
            SubnetRequest subnetRequest = new SubnetRequest();
            subnetRequest.setPrivateSubnetCidr(privateSubnetCidrs.get(i));
            subnetRequest.setAvailabilityZone(region);
            subnetRequest.setSubnetGroup(i % publicSubnetCidrs.size());
            subnetRequest.setIndex(index++);
            subnets.add(subnetRequest);
        }

        return subnets;
    }
}
