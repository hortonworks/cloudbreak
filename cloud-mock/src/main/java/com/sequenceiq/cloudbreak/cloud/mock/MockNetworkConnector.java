package com.sequenceiq.cloudbreak.cloud.mock;

import java.util.Set;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.NetworkConnector;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.Network;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Variant;
import com.sequenceiq.cloudbreak.cloud.model.network.CreatedCloudNetwork;
import com.sequenceiq.cloudbreak.cloud.model.network.CreatedSubnet;
import com.sequenceiq.cloudbreak.cloud.model.network.NetworkCreationRequest;
import com.sequenceiq.cloudbreak.cloud.model.network.NetworkDeletionRequest;

@Service
public class MockNetworkConnector implements NetworkConnector {
    @Override
    public CreatedCloudNetwork createNetworkWithSubnets(NetworkCreationRequest request) {
        CreatedSubnet subnet1 = new CreatedSubnet();
        CreatedSubnet subnet2 = new CreatedSubnet();
        subnet1.setAvailabilityZone("europe-a");
        subnet2.setAvailabilityZone("europe-b");
        subnet1.setCidr("0.0.0.0/0");
        subnet2.setCidr("0.0.0.0/0");
        subnet2.setPublicSubnet(true);
        subnet1.setSubnetId("1");
        subnet2.setSubnetId("2");

        CreatedCloudNetwork result = new CreatedCloudNetwork(request.getStackName(),
                "mockedNetwork1",
                Set.of(subnet1, subnet2));

        return result;
    }

    @Override
    public void deleteNetworkWithSubnets(NetworkDeletionRequest networkDeletionRequest) {

    }

    @Override
    public String getNetworkCidr(Network network, CloudCredential credential) {
        return "0.0.0.0/0";
    }

    @Override
    public Platform platform() {
        return MockConstants.MOCK_PLATFORM;
    }

    @Override
    public Variant variant() {
        return MockConstants.MOCK_VARIANT;
    }
}
