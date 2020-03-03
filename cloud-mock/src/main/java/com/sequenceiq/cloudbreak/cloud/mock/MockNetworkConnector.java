package com.sequenceiq.cloudbreak.cloud.mock;

import java.util.List;
import java.util.Set;

import javax.ws.rs.BadRequestException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.NetworkConnector;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.cloud.model.Network;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.SubnetSelectionParameters;
import com.sequenceiq.cloudbreak.cloud.model.Variant;
import com.sequenceiq.cloudbreak.cloud.model.network.CreatedCloudNetwork;
import com.sequenceiq.cloudbreak.cloud.model.network.CreatedSubnet;
import com.sequenceiq.cloudbreak.cloud.model.network.NetworkCreationRequest;
import com.sequenceiq.cloudbreak.cloud.model.network.NetworkDeletionRequest;
import com.sequenceiq.cloudbreak.cloud.model.SubnetSelectionResult;

@Service
public class MockNetworkConnector implements NetworkConnector {

    private static final Logger LOGGER = LoggerFactory.getLogger(MockNetworkConnector.class);

    @Override
    public CreatedCloudNetwork createNetworkWithSubnets(NetworkCreationRequest request) {
        CreatedSubnet subnet1 = new CreatedSubnet();
        CreatedSubnet subnet2 = new CreatedSubnet();
        subnet1.setAvailabilityZone("europe-a");
        subnet2.setAvailabilityZone("europe-b");
        subnet1.setCidr("10.1.0.0/16");
        subnet2.setCidr("10.2.0.0/16");
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
        return "10.0.0.0/8";
    }

    @Override
    public SubnetSelectionResult selectSubnets(List<CloudSubnet> subnetMetas, SubnetSelectionParameters subnetSelectionParameters) {
        if (subnetMetas == null || subnetMetas.isEmpty()) {
            String message = "Mock subnet selection: there are no subnets to choose from";
            LOGGER.debug(message);
            throw new BadRequestException(message);
        }
        return new SubnetSelectionResult(subnetMetas);
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
