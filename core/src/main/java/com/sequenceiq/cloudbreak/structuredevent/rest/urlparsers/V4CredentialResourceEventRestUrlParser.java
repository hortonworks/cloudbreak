package com.sequenceiq.cloudbreak.structuredevent.rest.urlparsers;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.structuredevent.rest.urlparser.LegacyRestUrlParser;

@Component
public class V4CredentialResourceEventRestUrlParser extends LegacyRestUrlParser {

    public static final int WORKSPACE_ID_GROUP_NUMBER = 1;

    public static final int RESOURCE_NAME_GROUP_NUMBER = 3;

    public static final int RESOURCE_EVENT_GROUP_NUMBER = 2;

    // Irregular requests with event but no resource name
    private static final Pattern ANTI_PATTERN = Pattern.compile("v4/\\d+/credentials/code_grant_flow/init");

    private static final Pattern PATTERN = Pattern.compile("v4/(\\d+)/credentials/(prerequisites|code_grant_flow/(?:authorization|init))/([^/]+)");

    @Override
    protected Pattern getAntiPattern() {
        return ANTI_PATTERN;
    }

    @Override
    public Pattern getPattern() {
        return PATTERN;
    }

    @Override
    protected String getWorkspaceId(Matcher matcher) {
        return matcher.group(WORKSPACE_ID_GROUP_NUMBER);
    }

    @Override
    protected String getResourceName(Matcher matcher) {
        return matcher.group(RESOURCE_NAME_GROUP_NUMBER);
    }

    @Override
    protected String getResourceCrn(Matcher matcher) {
        return null;
    }

    @Override
    protected String getResourceId(Matcher matcher) {
        return null;
    }

    @Override
    protected String getResourceType(Matcher matcher) {
        return "credentials";
    }

    @Override
    protected String getResourceEvent(Matcher matcher) {
        return matcher.group(RESOURCE_EVENT_GROUP_NUMBER);
    }

}
