package com.sequenceiq.cloudbreak.structuredevent.rest.urlparsers;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

@Component
public class V3CredentialCodeGrantFlowParser extends RestUrlParser {

    private static final int WORKSPACE_ID_GROUP_NUMBER = 1;

    private static final int RESOURCE_NAME_GROUP_NUMBER = 4;

    private static final int RESOURCE_EVENT_GROUP_NUMBER = 3;

    private static final int RESOURCE_TYPE_GROUP_NUMBER = 2;

    private static final Pattern PATTERN = Pattern.compile("v3/(\\d+)/([a-z|-]+)/codegrantflow/([a-z|-]+)/([^/]+)");

    @Override
    protected Pattern getPattern() {
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
    protected String getResourceId(Matcher matcher) {
        return null;
    }

    @Override
    protected String getResourceType(Matcher matcher) {
        return matcher.group(RESOURCE_TYPE_GROUP_NUMBER);
    }

    @Override
    protected String getResourceEvent(Matcher matcher) {
        return matcher.group(RESOURCE_EVENT_GROUP_NUMBER);
    }
}
