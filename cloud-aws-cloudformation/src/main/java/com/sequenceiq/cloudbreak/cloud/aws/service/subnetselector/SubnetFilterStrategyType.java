package com.sequenceiq.cloudbreak.cloud.aws.service.subnetselector;

public enum SubnetFilterStrategyType {
    MULTIPLE_PREFER_PRIVATE("choose multiple subnets in different AZs prefer private"),
    MULTIPLE_PREFER_PUBLIC("choose multiple subnets in different AZs prefer public");

    private final String description;

    SubnetFilterStrategyType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
