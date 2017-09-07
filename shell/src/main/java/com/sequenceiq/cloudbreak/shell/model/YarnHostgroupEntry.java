package com.sequenceiq.cloudbreak.shell.model;

public class YarnHostgroupEntry implements NodeCountEntry {

    private final Integer nodeCount;

    private final String constraintName;

    public YarnHostgroupEntry(Integer nodeCount, String constraintName) {
        this.nodeCount = nodeCount;
        this.constraintName = constraintName;
    }

    public String getConstraintName() {
        return constraintName;
    }

    @Override
    public Integer getNodeCount() {
        return nodeCount;
    }
}
