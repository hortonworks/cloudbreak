package com.sequenceiq.cloudbreak.converter.v4.environment.network;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.environment.api.v1.environment.model.EnvironmentNetworkGcpParams;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentNetworkResponse;

@Component
public class GcpEnvironmentNetworkConverter extends EnvironmentBaseNetworkConverter {

    @Override
    Map<String, Object> getAttributesForLegacyNetwork(EnvironmentNetworkResponse source) {
        EnvironmentNetworkGcpParams gcp = source.getGcp();
        return Map.of(
                "networkId", gcp.getNetworkId(),
                "noFirewallRules", gcp.getNoFirewallRules(),
                "noPublicIp", gcp.getNoPublicIp(),
                "sharedProjectId", gcp.getSharedProjectId());
    }

    @Override
    public CloudPlatform getCloudPlatform() {
        return CloudPlatform.GCP;
    }
}
