package com.sequenceiq.cloudbreak.service.loadbalancer;

import java.util.Map;
import java.util.Optional;

import com.sequenceiq.cloudbreak.common.json.Json;

final class NetworkLoadBalancerAttributeUtil {

    private NetworkLoadBalancerAttributeUtil() {
    }

    static boolean isSessionStickyForTargetGroup(Json networkAttributes) {
        return getLoadBalancerAttributeIfExists(networkAttributes)
                .map(map -> (Boolean) map.get("stickySessionForLoadBalancerTarget"))
                .orElse(false);
    }

    static Optional<Map<String, Object>> getLoadBalancerAttributeIfExists(Json networkAttributes) {
        return Optional.ofNullable(networkAttributes)
                .map(Json::getMap)
                .map(map -> (Map<String, Object>) map.get("loadBalancer"));
    }

}
