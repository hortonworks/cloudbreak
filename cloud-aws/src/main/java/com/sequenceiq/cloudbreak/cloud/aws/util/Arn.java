package com.sequenceiq.cloudbreak.cloud.aws.util;

import com.amazonaws.util.StringUtils;

public class Arn {

    private final String partition;

    private final String service;

    private final String region;

    private final String accountId;

    private final String resource;

    private Arn(String partition, String service, String region, String accountId, String resource) {
        this.partition = partition;
        this.service = service;
        this.region = region;
        this.accountId = accountId;
        this.resource = resource;
    }

    public String getPartition() {
        return partition;
    }

    public String getService() {
        return service;
    }

    public String getRegion() {
        return region;
    }

    public String getAccountId() {
        return accountId;
    }

    public String getResource() {
        return resource;
    }

    public static Arn of(String arnString) {
        if (StringUtils.isNullOrEmpty(arnString)) {
            throw new IllegalArgumentException("ARN must not be empty.");
        }

        String[] parts = arnString.split(":", -1);

        //CHECKSTYLE:OFF: checkstyle:magicnumber
        if (parts.length != 5) {
            throw new IllegalArgumentException("ARN must consist of exactly 5 parts.");
        }

        return new Arn(parts[0], parts[1], parts[2], parts[3], parts[4]);
        //CHECKSTYLE:ON: checkstyle:magicnumber
    }
}
