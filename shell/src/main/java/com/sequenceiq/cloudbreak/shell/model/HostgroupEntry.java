package com.sequenceiq.cloudbreak.shell.model;

import java.util.HashSet;
import java.util.Set;

import com.sequenceiq.cloudbreak.api.model.RecoveryMode;

public class HostgroupEntry implements NodeCountEntry {

    private final Integer nodeCount;

    private Set<Long> recipeIdSet = new HashSet<>();

    private RecoveryMode recoveryMode = RecoveryMode.MANUAL;

    public HostgroupEntry(Integer nodeCount, Set<Long> recipeIdSet, RecoveryMode recoveryMode) {
        this.nodeCount = nodeCount;
        this.recipeIdSet = recipeIdSet;
        this.recoveryMode = recoveryMode;
    }

    @Override
    public Integer getNodeCount() {
        return nodeCount;
    }

    public Set<Long> getRecipeIdSet() {
        return recipeIdSet;
    }

    public RecoveryMode getRecoveryMode() {
        return recoveryMode;
    }
}
