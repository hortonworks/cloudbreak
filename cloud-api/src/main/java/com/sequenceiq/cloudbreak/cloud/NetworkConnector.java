package com.sequenceiq.cloudbreak.cloud;

import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.Network;
import com.sequenceiq.cloudbreak.cloud.model.network.CreatedCloudNetwork;
import com.sequenceiq.cloudbreak.cloud.model.network.NetworkCreationRequest;
import com.sequenceiq.cloudbreak.cloud.model.network.NetworkDeletionRequest;

/**
 * Network connectors.
 */
public interface NetworkConnector extends CloudPlatformAware {

    CreatedCloudNetwork createNetworkWithSubnets(NetworkCreationRequest networkCreationRequest);

    void deleteNetworkWithSubnets(NetworkDeletionRequest networkDeletionRequest);

    String getNetworkCidr(Network network, CloudCredential credential);
}
