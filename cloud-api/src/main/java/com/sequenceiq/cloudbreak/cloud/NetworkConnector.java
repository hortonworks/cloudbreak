package com.sequenceiq.cloudbreak.cloud;

import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.cloud.model.Network;
import com.sequenceiq.cloudbreak.cloud.model.SubnetSelectionParameters;
import com.sequenceiq.cloudbreak.cloud.model.SubnetSelectionResult;
import com.sequenceiq.cloudbreak.cloud.model.network.CreatedCloudNetwork;
import com.sequenceiq.cloudbreak.cloud.model.network.NetworkCreationRequest;
import com.sequenceiq.cloudbreak.cloud.model.network.NetworkDeletionRequest;

/**
 * Network connectors.
 */
public interface NetworkConnector extends CloudPlatformAware {
    Logger LOGGER = LoggerFactory.getLogger(NetworkConnector.class);

    CreatedCloudNetwork createNetworkWithSubnets(NetworkCreationRequest networkCreationRequest);

    void deleteNetworkWithSubnets(NetworkDeletionRequest networkDeletionRequest);

    String getNetworkCidr(Network network, CloudCredential credential);

    SubnetSelectionResult chooseSubnets(Collection<CloudSubnet> subnetMetas, SubnetSelectionParameters subnetSelectionParameters);
}
