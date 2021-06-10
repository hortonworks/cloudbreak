package com.sequenceiq.freeipa.service.filter;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.common.network.NetworkConstants;
import com.sequenceiq.freeipa.entity.Network;

@Service
public class GcpNetworkFilterProvider implements NetworkFilterProvider {

    private static final String SHARED_PROJECT_ID = "sharedProjectId";

    @Override
    public Map<String, String> provide(Network network, String networkId, Collection<String> subnetIds) {
        Map<String, String> filter = new HashMap<>();
        if (network.getAttributes() != null && network.getAttributes().getMap() != null) {
            Map<String, Object> attributes = network.getAttributes().getMap();
            String sharedProjectId = (String) attributes.get(SHARED_PROJECT_ID);
            if (!Strings.isNullOrEmpty(sharedProjectId)) {
                filter.put(SHARED_PROJECT_ID, sharedProjectId);
            }
        }
        filter.put("networkId", networkId);
        filter.putAll(buildSubnetIdFilter(subnetIds));
        return filter;
    }

    @Override
    public CloudPlatform cloudPlatform() {
        return CloudPlatform.GCP;
    }

    private Map<String, String> buildSubnetIdFilter(Collection<String> subnetIds) {
        Map<String, String> filter = new HashMap<>();
        if (subnetIds != null && !subnetIds.isEmpty()) {
            filter.put(NetworkConstants.SUBNET_IDS, String.join(",", subnetIds));
        }
        return filter;
    }
}
