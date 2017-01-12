package com.sequenceiq.cloudbreak.shell.model;

public class YarnHostgroupEntry implements NodeCountEntry {

    private Integer nodeCount;

    private String constraintName;

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
