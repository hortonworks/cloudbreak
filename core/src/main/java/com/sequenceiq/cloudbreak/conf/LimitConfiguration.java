package com.sequenceiq.cloudbreak.conf;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import javax.inject.Inject;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;

@Service
@ConfigurationProperties(prefix = "cb.limit")
public class LimitConfiguration {

    private static final String UPGRADE_NODE_COUNT_LIMIT_PROPERTY = "upgrade";

    private static final int DEFAULT_UPGRADE_NODE_COUNT_LIMIT = 200;

    private static final String SAFE_LIMITS_KEY = "safe";

    private static final String EXPERIMENTAL_LIMITS_KEY = "experimental";

    private Map<String, Map<String, Integer>> nodeCountLimits;

    @Inject
    private EntitlementService entitlementService;

    public Map<String, Map<String, Integer>> getNodeCountLimits() {
        return nodeCountLimits;
    }

    public void setNodeCountLimits(Map<String, Map<String, Integer>> nodeCountLimits) {
        this.nodeCountLimits = nodeCountLimits;
    }

    public Integer getNodeCountLimit(Optional<String> accountId) {
        return nodeCountLimits.get(getLimitsKey(accountId)).entrySet()
                .stream()
                .filter(e -> !UPGRADE_NODE_COUNT_LIMIT_PROPERTY.equals(e.getKey()))
                .map(Entry::getValue)
                .min(Integer::compareTo)
                .orElse(Integer.MAX_VALUE);
    }

    public Integer getUpgradeNodeCountLimit(Optional<String> accountId) {
        return nodeCountLimits.get(getLimitsKey(accountId))
                .getOrDefault(UPGRADE_NODE_COUNT_LIMIT_PROPERTY, DEFAULT_UPGRADE_NODE_COUNT_LIMIT);
    }

    private String getLimitsKey(Optional<String> accountId) {
        try {
            if (accountId.isPresent() && entitlementService.isExperimentalNodeCountLimitsEnabled(accountId.get())) {
                return EXPERIMENTAL_LIMITS_KEY;
            }
        } catch (Exception ignored) {

        }
        return SAFE_LIMITS_KEY;
    }
}
