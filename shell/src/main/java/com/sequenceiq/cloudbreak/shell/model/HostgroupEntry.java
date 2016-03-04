package com.sequenceiq.cloudbreak.shell.model;

import java.util.HashSet;
import java.util.Set;

public class HostgroupEntry implements NodeCountEntry {

    private Integer nodeCount;
    private Set<Long> recipeIdSet = new HashSet<>();

    public HostgroupEntry(Integer nodeCount, Set<Long> recipeIdSet) {
        this.nodeCount = nodeCount;
        this.recipeIdSet = recipeIdSet;
    }

    @Override
    public Integer getNodeCount() {
        return nodeCount;
    }

    public Set<Long> getRecipeIdSet() {
        return recipeIdSet;
    }
}
