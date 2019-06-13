package com.sequenceiq.cloudbreak.cloud;

import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.network.CreatedCloudNetwork;
import com.sequenceiq.cloudbreak.cloud.model.network.NetworkCreationRequest;

/**
 * Network connectors.
 */
public interface NetworkConnector extends CloudPlatformAware {

    CreatedCloudNetwork createNetworkWithSubnets(NetworkCreationRequest networkCreationRequest);

    CreatedCloudNetwork deleteNetworkWithSubnets(CloudCredential cloudCredential);
}
