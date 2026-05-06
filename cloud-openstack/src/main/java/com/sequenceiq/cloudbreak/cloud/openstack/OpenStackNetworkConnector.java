package com.sequenceiq.cloudbreak.cloud.openstack;

import static com.sequenceiq.cloudbreak.cloud.openstack.common.OpenStackConstants.NETWORK_ID;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;

import org.openstack4j.api.OSClient;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.DefaultNetworkConnector;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.cloud.model.Network;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.SubnetSelectionParameters;
import com.sequenceiq.cloudbreak.cloud.model.SubnetSelectionResult;
import com.sequenceiq.cloudbreak.cloud.model.Variant;
import com.sequenceiq.cloudbreak.cloud.model.network.CreatedCloudNetwork;
import com.sequenceiq.cloudbreak.cloud.model.network.NetworkCreationRequest;
import com.sequenceiq.cloudbreak.cloud.model.network.NetworkDeletionRequest;
import com.sequenceiq.cloudbreak.cloud.network.NetworkCidr;
import com.sequenceiq.cloudbreak.cloud.openstack.client.OpenStackClient;
import com.sequenceiq.cloudbreak.cloud.openstack.common.OpenStackConstants;

@Component
public class OpenStackNetworkConnector implements DefaultNetworkConnector {

    @Inject
    private OpenStackClient openStackClient;

    @Override
    public Platform platform() {
        return OpenStackConstants.OPENSTACK_PLATFORM;
    }

    @Override
    public Variant variant() {
        return OpenStackConstants.OPENSTACK_VARIANT;
    }

    @Override
    public SubnetSelectionResult filterSubnets(Collection<CloudSubnet> subnetMetas, SubnetSelectionParameters subnetSelectionParameters) {
        return new SubnetSelectionResult(new ArrayList<>(subnetMetas));
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
    public CreatedCloudNetwork createNetworkWithSubnets(NetworkCreationRequest networkCreationRequest) {
        throw new UnsupportedOperationException("OpenStack does not support network creation");
    }

    @Override
    public void deleteNetworkWithSubnets(NetworkDeletionRequest networkDeletionRequest) {
        throw new UnsupportedOperationException("OpenStack does not support network deletion");
    }

    @Override
    public NetworkCidr getNetworkCidr(Network network, CloudCredential credential) {
        String networkId = network.getStringParameter(NETWORK_ID);
        if (networkId == null) {
            throw new BadRequestException("Network id must be provided for OpenStack");
        }
        OSClient<?> osClient = openStackClient.createOSClient(credential);
        org.openstack4j.model.network.Network openstackNetwork = osClient.networking().network().get(networkId);
        if (openstackNetwork == null) {
            throw new BadRequestException("Network not found with id: " + networkId);
        }
        List<String> subnets = openstackNetwork.getSubnets().stream()
                .map(subnet -> osClient.networking().subnet().get(subnet).getCidr())
                .toList();
        if (subnets.isEmpty()) {
            throw new BadRequestException("No subnets found for network: " + networkId);
        }
        // TODO: Openstack - one subnet supported for now
        return new NetworkCidr(subnets.getFirst());
    }
}
