package com.sequenceiq.cloudbreak.shell.model;

import java.util.HashSet;
import java.util.Set;

public class HostgroupEntry {

    private Integer nodeCount;
    private Set<Long> recipeIdSet = new HashSet<>();
    private String constraintName;

    public HostgroupEntry(Integer nodeCount, Set<Long> recipeIdSet) {
        this.nodeCount = nodeCount;
        this.recipeIdSet = recipeIdSet;
    }

    public HostgroupEntry(Integer nodeCount, String constraintName) {
        this.nodeCount = nodeCount;
        this.constraintName = constraintName;
    }

    public String getConstraintName() {
        return constraintName;
    }

    public Integer getNodeCount() {
        return nodeCount;
    }

    public Set<Long> getRecipeIdSet() {
        return recipeIdSet;
    }
}
