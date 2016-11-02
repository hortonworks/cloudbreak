package com.sequenceiq.cloudbreak.controller.validation.blueprint;

public class StackServiceComponentDescriptor {
    private static final String MASTER = "MASTER";

    private String name;

    private String category;

    private int minCardinality;

    private int maxCardinality;

    public StackServiceComponentDescriptor(String name, String category, int minCardinality, int maxCardinality) {
        this.name = name;
        this.category = category;
        this.minCardinality = minCardinality;
        this.maxCardinality = maxCardinality;
    }

    public String getName() {
        return name;
    }

    public String getCategory() {
        return category;
    }

    public int getMinCardinality() {
        return minCardinality;
    }

    public int getMaxCardinality() {
        return maxCardinality;
    }

    public boolean isMaster() {
        return MASTER.equals(category);
    }
}
