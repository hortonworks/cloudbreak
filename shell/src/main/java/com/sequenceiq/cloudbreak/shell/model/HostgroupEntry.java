package com.sequenceiq.cloudbreak.shell.model;

import java.util.Set;

public class HostgroupEntry {

    private Integer nodeCount;
    private Set<Long> recipeIdSet;

    public HostgroupEntry(Integer nodeCount, Set<Long> recipeIdSet) {
        this.recipeIdSet = recipeIdSet;
        this.nodeCount = nodeCount;
        this.recipeIdSet = recipeIdSet;
    }

    public Integer getNodeCount() {
        return nodeCount;
    }

    public Set<Long> getRecipeIdSet() {
        return recipeIdSet;
    }
}
