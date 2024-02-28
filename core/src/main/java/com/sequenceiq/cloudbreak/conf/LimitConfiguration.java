package com.sequenceiq.cloudbreak.conf;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;

@Service
@ConfigurationProperties(prefix = "cb.limit")
public class LimitConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(LimitConfiguration.class);

    private static final String UPGRADE_NODE_COUNT_LIMIT_PROPERTY = "upgrade";

    private static final int DEFAULT_UPGRADE_NODE_COUNT_LIMIT = 200;

    private static final String SAFE_LIMITS_KEY = "safe";

    private static final String EXPERIMENTAL_LIMITS_KEY = "experimental";

    private Map<String, Map<String, Integer>> nodeCountLimits;

    private List<PrimaryGatewayRequirement> primaryGatewayRecommendations;

    @Inject
    private EntitlementService entitlementService;

    public Map<String, Map<String, Integer>> getNodeCountLimits() {
        return nodeCountLimits;
    }

    public void setNodeCountLimits(Map<String, Map<String, Integer>> nodeCountLimits) {
        this.nodeCountLimits = nodeCountLimits;
    }

    public List<PrimaryGatewayRequirement> getPrimaryGatewayRecommendations() {
        return primaryGatewayRecommendations;
    }

    public void setPrimaryGatewayRecommendations(List<PrimaryGatewayRequirement> primaryGatewayRecommendations) {
        this.primaryGatewayRecommendations = primaryGatewayRecommendations;
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

    public Optional<PrimaryGatewayRequirement> getPrimaryGatewayRequirement(Integer nodeCount) {
        PrimaryGatewayRequirement primaryGatewayRequirement = null;
        for (PrimaryGatewayRequirement nodeRequirement : primaryGatewayRecommendations) {
            if (nodeRequirement.getNodeCount() <= nodeCount) {
                primaryGatewayRequirement = nodeRequirement;
            } else {
                return Optional.ofNullable(primaryGatewayRequirement);
            }
        }
        return Optional.ofNullable(primaryGatewayRequirement);
    }

    private String getLimitsKey(Optional<String> accountId) {
        try {
            if (accountId.isPresent() && entitlementService.isExperimentalNodeCountLimitsEnabled(accountId.get())) {
                return EXPERIMENTAL_LIMITS_KEY;
            }
        } catch (Exception ignored) {
            LOGGER.info("exception happened {}", ignored);
        }
        return SAFE_LIMITS_KEY;
    }
}
