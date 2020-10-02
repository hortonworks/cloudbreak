package com.sequenceiq.cloudbreak.converter.v4.environment.network;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.environment.api.v1.environment.model.EnvironmentNetworkGcpParams;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentNetworkResponse;

@Component
public class GcpEnvironmentNetworkConverter extends EnvironmentBaseNetworkConverter {

    @Override
    Map<String, Object> getAttributesForLegacyNetwork(EnvironmentNetworkResponse source) {
        EnvironmentNetworkGcpParams gcp = source.getGcp();
        Map<String, Object> result = new HashMap<>();
        if (!Strings.isNullOrEmpty(gcp.getNetworkId())) {
            result.put("networkId", gcp.getNetworkId());
        }
        if (gcp.getNoFirewallRules() != null) {
            result.put("noFirewallRules", gcp.getNoFirewallRules());
        }
        if (gcp.getNoPublicIp() != null) {
            result.put("noPublicIp", gcp.getNoPublicIp());
        }
        if (!Strings.isNullOrEmpty(gcp.getSharedProjectId())) {
            result.put("sharedProjectId", gcp.getSharedProjectId());
        }
        return result;
    }

    @Override
    public CloudPlatform getCloudPlatform() {
        return CloudPlatform.GCP;
    }
}
