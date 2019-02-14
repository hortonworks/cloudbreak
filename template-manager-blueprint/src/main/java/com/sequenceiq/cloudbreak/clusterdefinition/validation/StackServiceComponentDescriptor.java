package com.sequenceiq.cloudbreak.clusterdefinition.validation;

public class StackServiceComponentDescriptor {

    private static final String MASTER = "MASTER";

    private final String name;

    private final String category;

    private final int minCardinality;

    private final int maxCardinality;

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
