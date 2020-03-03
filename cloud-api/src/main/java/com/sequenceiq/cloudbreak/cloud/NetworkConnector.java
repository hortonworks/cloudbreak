package com.sequenceiq.cloudbreak.cloud;

import java.util.List;

import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.cloud.model.Network;
import com.sequenceiq.cloudbreak.cloud.model.SubnetSelectionParameters;
import com.sequenceiq.cloudbreak.cloud.model.network.CreatedCloudNetwork;
import com.sequenceiq.cloudbreak.cloud.model.network.NetworkCreationRequest;
import com.sequenceiq.cloudbreak.cloud.model.network.NetworkDeletionRequest;
import com.sequenceiq.cloudbreak.cloud.model.SubnetSelectionResult;

/**
 * Network connectors.
 */
public interface NetworkConnector extends CloudPlatformAware {

    CreatedCloudNetwork createNetworkWithSubnets(NetworkCreationRequest networkCreationRequest);

    void deleteNetworkWithSubnets(NetworkDeletionRequest networkDeletionRequest);

    String getNetworkCidr(Network network, CloudCredential credential);

    SubnetSelectionResult selectSubnets(List<CloudSubnet> subnetMetas, SubnetSelectionParameters subnetSelectionParameters);
}
