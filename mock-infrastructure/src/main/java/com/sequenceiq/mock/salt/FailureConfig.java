package com.sequenceiq.mock.salt;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class FailureConfig {

    private final Map<CommandGroupPair, FailedNodeConfig> failedNodeConfigs;

    public FailureConfig() {
        failedNodeConfigs = new ConcurrentHashMap<>();
    }

    public void setFailedNodeConfig(String command, String group, int allNodeCount, int failedNodeCount) {
        failedNodeConfigs.put(new CommandGroupPair(command, group), new FailedNodeConfig(allNodeCount, failedNodeCount));
    }

    public FailedNodeConfig getFailedNodeConfig(String command, String group) {
        return failedNodeConfigs.get(new CommandGroupPair(command, group));
    }

    private record CommandGroupPair(String command, String group) {
    }


    public record FailedNodeConfig(int allNodeCount, int failedNodeCount) {

    }
}
