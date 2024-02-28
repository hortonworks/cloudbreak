package com.sequenceiq.cloudbreak.conf;

import java.util.Map;

public class PrimaryGatewayRequirement {

    private Integer nodeCount;

    private Integer minCpu;

    private Integer minMemory;

    private Map<String, String> recommendedInstance;

    public Integer getNodeCount() {
        return nodeCount;
    }

    public void setNodeCount(Integer nodeCount) {
        this.nodeCount = nodeCount;
    }

    public Integer getMinCpu() {
        return minCpu;
    }

    public void setMinCpu(Integer minCpu) {
        this.minCpu = minCpu;
    }

    public Integer getMinMemory() {
        return minMemory;
    }

    public void setMinMemory(Integer minMemory) {
        this.minMemory = minMemory;
    }

    public Map<String, String> getRecommendedInstance() {
        return recommendedInstance;
    }

    public void setRecommendedInstance(Map<String, String> recommendedInstance) {
        this.recommendedInstance = recommendedInstance;
    }
}
