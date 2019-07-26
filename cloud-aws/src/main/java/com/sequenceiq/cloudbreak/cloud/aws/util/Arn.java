package com.sequenceiq.cloudbreak.cloud.aws.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

public class Arn {

    private static final Pattern ARN_PATTERN = Pattern.compile("^arn:(.+?):(.+?):(.*?):(.*?):(.+)$");

    private final String prefix;

    private final String partition;

    private final String service;

    private final String region;

    private final String accountId;

    private final String resource;

    private Arn(String prefix, String partition, String service, String region, String accountId, String resource) {
        this.prefix = prefix;
        this.partition = partition;
        this.service = service;
        this.region = region;
        this.accountId = accountId;
        this.resource = resource;
    }

    public String getPrefix() {
        return prefix;
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
        if (StringUtils.isBlank(arnString)) {
            throw new IllegalArgumentException("ARN must not be empty.");
        }
        Matcher matcher = ARN_PATTERN.matcher(arnString);
        if (matcher.find()) {
            //CHECKSTYLE:OFF: checkstyle:magicnumber
            return new Arn("arn", matcher.group(1), matcher.group(2), matcher.group(3), matcher.group(4), matcher.group(5));
            //CHECKSTYLE:ON: checkstyle:magicnumber
        }
        throw new IllegalArgumentException("ARN has invalid format.");
    }
}
