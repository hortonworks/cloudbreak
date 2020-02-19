package com.sequenceiq.cloudbreak.cloud.aws.service.subnetselector;

public enum SubnetSelectorStrategyType {
    SINGLE_PREFER_PRIVATE("choose single subnet prefer private"),
    SINGLE_PREFER_PUBLIC("choose single subnet prefer public"),

    MULTIPLE_PREFER_PRIVATE("choose multiple subnets in different AZs prefer private"),
    MULTIPLE_PREFER_PUBLIC("choose multiple subnets in different AZs prefer public");

    private final String description;

    SubnetSelectorStrategyType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
