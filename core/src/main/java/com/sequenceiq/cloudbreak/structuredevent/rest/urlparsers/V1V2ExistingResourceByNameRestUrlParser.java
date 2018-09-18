package com.sequenceiq.cloudbreak.structuredevent.rest.urlparsers;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

@Component
public class V1V2ExistingResourceByNameRestUrlParser extends RestUrlParser {

    public static final int RESOURCE_NAME_GROUP_NUMBER = 4;

    public static final int RESOURCE_TYPE_GROUP_NUMBER = 1;

    private static final Pattern ANTI_PATTERN = Pattern.compile(".*("
            + "repositoryconfigs/validate|"
            + "rdsconfigs/testconnect|"
            + "ldap/testconnect|"
            + "stacks/validate|"
            + "stacks/blueprint|"
            + "users/evict|"
            + "util|"
            + "connectors"
            + ").*");

    private static final Pattern PATTERN = Pattern.compile("v[12]/([a-z|-]*)(/(user|account))?/(?!account|user)([^/\\d]+)");

    @Override
    public Pattern getPattern() {
        return PATTERN;
    }

    @Override
    public Pattern getAntiPattern() {
        return ANTI_PATTERN;
    }

    @Override
    protected String getWorkspaceId(Matcher matcher) {
        return null;
    }

    @Override
    protected String getResourceName(Matcher matcher) {
        return matcher.group(RESOURCE_NAME_GROUP_NUMBER);
    }

    @Override
    protected String getResourceId(Matcher matcher) {
        return null;
    }

    @Override
    protected String getResourceType(Matcher matcher) {
        return matcher.group(RESOURCE_TYPE_GROUP_NUMBER);
    }

    @Override
    protected String getResourceEvent(Matcher matcher) {
        return null;
    }
}
