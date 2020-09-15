package com.sequenceiq.cloudbreak.cloud.mock;

import static com.sequenceiq.cloudbreak.cloud.model.network.SubnetType.PUBLIC;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

import javax.ws.rs.BadRequestException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.DefaultNetworkConnector;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.cloud.model.Network;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.SubnetSelectionParameters;
import com.sequenceiq.cloudbreak.cloud.model.SubnetSelectionResult;
import com.sequenceiq.cloudbreak.cloud.model.Variant;
import com.sequenceiq.cloudbreak.cloud.model.network.CreatedCloudNetwork;
import com.sequenceiq.cloudbreak.cloud.model.network.CreatedSubnet;
import com.sequenceiq.cloudbreak.cloud.model.network.NetworkCreationRequest;
import com.sequenceiq.cloudbreak.cloud.model.network.NetworkDeletionRequest;
import com.sequenceiq.cloudbreak.cloud.network.NetworkCidr;

@Service
public class MockNetworkConnector implements DefaultNetworkConnector {

    private static final Logger LOGGER = LoggerFactory.getLogger(MockNetworkConnector.class);

    @Override
    public CreatedCloudNetwork createNetworkWithSubnets(NetworkCreationRequest request) {
        CreatedSubnet subnet1 = new CreatedSubnet();
        CreatedSubnet subnet2 = new CreatedSubnet();
        subnet1.setAvailabilityZone("europe-a");
        subnet2.setAvailabilityZone("europe-b");
        subnet1.setCidr("172.16.0.0/16");
        subnet2.setCidr("172.17.0.0/16");
        subnet2.setPublicSubnet(true);
        subnet1.setSubnetId("1");
        subnet2.setSubnetId("2");
        subnet1.setType(PUBLIC);
        subnet2.setType(PUBLIC);

        CreatedCloudNetwork result = new CreatedCloudNetwork(request.getStackName(),
                "mockedNetwork1",
                Set.of(subnet1, subnet2));

        return result;
    }

    @Override
    public void deleteNetworkWithSubnets(NetworkDeletionRequest networkDeletionRequest) {

    }

    @Override
    public NetworkCidr getNetworkCidr(Network network, CloudCredential credential) {
        return  new NetworkCidr("172.16.0.0/16");
    }

    @Override
    public SubnetSelectionResult filterSubnets(Collection<CloudSubnet> subnetMetas, SubnetSelectionParameters subnetSelectionParameters) {
        if (subnetMetas == null || subnetMetas.isEmpty()) {
            String message = "Mock subnet selection: there are no subnets to choose from";
            LOGGER.debug(message);
            throw new BadRequestException(message);
        }
        return new SubnetSelectionResult(subnetMetas.stream().collect(Collectors.toList()));
    }

    @Override
    public int subnetCountInDifferentAzMin() {
        return 1;
    }

    @Override
    public int subnetCountInDifferentAzMax() {
        return 1;
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
