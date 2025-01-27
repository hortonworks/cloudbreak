package com.sequenceiq.cloudbreak.template.model;

public enum HybridHostGroups {
    ECS_MASTER,
    ECS_WORKER;

    public static boolean isHybridHostGroup(String hostGroup) {
        return ECS_MASTER.name().equalsIgnoreCase(hostGroup) || ECS_WORKER.name().equalsIgnoreCase(hostGroup);
    }

    public static boolean isNotHybridHostGroup(String hostGroup) {
        return !isHybridHostGroup(hostGroup);
    }
}
