package com.sequenceiq.cloudbreak.cloud.mock;

import static com.sequenceiq.cloudbreak.cloud.model.network.SubnetType.PUBLIC;

import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.ws.rs.BadRequestException;

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
import com.sequenceiq.cloudbreak.cloud.model.network.NetworkSubnetRequest;
import com.sequenceiq.cloudbreak.cloud.network.NetworkCidr;

@Service
public class MockNetworkConnector implements DefaultNetworkConnector {

    private static final Logger LOGGER = LoggerFactory.getLogger(MockNetworkConnector.class);

    @Override
    public CreatedCloudNetwork createNetworkWithSubnets(NetworkCreationRequest request) {
        Set<CreatedSubnet> subnets = new HashSet<>();
        request.getPublicSubnets().forEach(s -> {
            createAndAddSubnet(subnets, s);
        });
        request.getPrivateSubnets().forEach(s -> {
            createAndAddSubnet(subnets, s);
        });

        return new CreatedCloudNetwork(request.getStackName(), "vpc1", subnets);
    }

    private void createAndAddSubnet(Set<CreatedSubnet> subnets, NetworkSubnetRequest subnetRequest) {
        int index = subnets.size();
        CreatedSubnet subnet = new CreatedSubnet();
        subnet.setCidr(subnetRequest.getCidr());
        subnet.setPublicSubnet(subnetRequest.getType() == PUBLIC);
        subnet.setSubnetId(subnetRequest.getType().name().toLowerCase(Locale.ROOT) + "_" + index);
        subnet.setType(subnetRequest.getType());
        int azCount = MockPlatformResources.LONDON_AVAILABILITY_ZONES.length;
        subnet.setAvailabilityZone(MockPlatformResources.LONDON_AVAILABILITY_ZONES[index % azCount]);
        subnets.add(subnet);
    }

    @Override
    public void deleteNetworkWithSubnets(NetworkDeletionRequest networkDeletionRequest) {

    }

    @Override
    public NetworkCidr getNetworkCidr(Network network, CloudCredential credential) {
        return new NetworkCidr("192.168.0.0/16");
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
