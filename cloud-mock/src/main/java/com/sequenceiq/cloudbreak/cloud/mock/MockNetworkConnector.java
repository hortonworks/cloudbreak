package com.sequenceiq.cloudbreak.cloud.mock;

import static com.sequenceiq.cloudbreak.cloud.model.network.SubnetType.PUBLIC;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.core.Response;

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

    private static final String FALLBACK_NETWORK_CIDR = "192.168.0.0/16";

    @Inject
    private MockUrlFactory mockUrlFactory;

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
        try (Response response = mockUrlFactory.get("/spi/get_network_cidr").get()) {
            if (response.getStatus() != 200) {
                LOGGER.warn("Mock-infrastructure returned status {} for /spi/get_network_cidr, falling back to {}",
                        response.getStatus(), FALLBACK_NETWORK_CIDR);
                return new NetworkCidr(FALLBACK_NETWORK_CIDR);
            }
            Map<String, Object> entity = response.readEntity(Map.class);
            if (entity == null || entity.get("cidr") == null) {
                return new NetworkCidr(FALLBACK_NETWORK_CIDR);
            }
            String cidr = entity.get("cidr").toString();
            Object cidrsRaw = entity.get("cidrs");
            if (cidrsRaw instanceof List<?> cidrsList && !cidrsList.isEmpty()) {
                List<String> cidrs = cidrsList.stream().map(Object::toString).toList();
                return new NetworkCidr(cidr, cidrs);
            }
            return new NetworkCidr(cidr);
        } catch (Exception e) {
            LOGGER.warn("Failed to fetch network CIDR from mock-infrastructure, falling back to {}: {}", FALLBACK_NETWORK_CIDR, e.getMessage());
            return new NetworkCidr(FALLBACK_NETWORK_CIDR);
        }
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
