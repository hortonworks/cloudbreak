package com.sequenceiq.cloudbreak.cloud.openstack.heat;

import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.NetworkConnector;
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
import com.sequenceiq.cloudbreak.cloud.model.network.NetworkResourcesCreationRequest;
import com.sequenceiq.cloudbreak.cloud.network.NetworkCidr;
import com.sequenceiq.cloudbreak.cloud.openstack.common.OpenStackConstants;
import com.sequenceiq.cloudbreak.cloud.openstack.nativ.OpenStackResourceException;

@Component
public class OpenStackHeatNetworkConnector implements NetworkConnector {

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenStackHeatNetworkConnector.class);

    @Override
    public CreatedCloudNetwork createNetworkWithSubnets(NetworkCreationRequest networkCreationRequest) {
        throw new OpenStackResourceException("CDP does not support Openstack.");
    }

    @Override
    public void deleteNetworkWithSubnets(NetworkDeletionRequest networkDeletionRequest) {
        throw new OpenStackResourceException("CDP does not support Openstack.");
    }

    @Override
    public NetworkCidr getNetworkCidr(Network network, CloudCredential credential) {
        throw new OpenStackResourceException("CDP does not support Openstack.");
    }

    @Override
    public SubnetSelectionResult chooseSubnets(Collection<CloudSubnet> subnetMetas, SubnetSelectionParameters subnetSelectionParameters) {
        throw new OpenStackResourceException("CDP does not support Openstack.");
    }

    @Override
    public void createProviderSpecificNetworkResources(NetworkResourcesCreationRequest networkResourcesCreationRequest) {
        throw new OpenStackResourceException("CDP does not support Openstack.");
    }

    @Override
    public SubnetSelectionResult chooseSubnetsForPrivateEndpoint(Collection<CloudSubnet> subnetMetas) {
        throw new OpenStackResourceException("CDP does not support Openstack.");
    }

    @Override
    public Platform platform() {
        return OpenStackConstants.OPENSTACK_PLATFORM;
    }

    @Override
    public Variant variant() {
        return OpenStackConstants.OpenStackVariant.HEAT.variant();
    }
}
