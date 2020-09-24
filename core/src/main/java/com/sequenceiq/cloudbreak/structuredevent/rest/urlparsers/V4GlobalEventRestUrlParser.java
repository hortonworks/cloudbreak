package com.sequenceiq.cloudbreak.structuredevent.rest.urlparsers;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.structuredevent.rest.urlparser.LegacyRestUrlParser;

@Component
public class V4GlobalEventRestUrlParser extends LegacyRestUrlParser {

    public static final int RESOURCE_TYPE_GROUP_NUMBER = 1;

    public static final int RESOURCE_EVENT_GROUP_NUMBER = 2;

    // v4/users/{event}
    // v4/utils/{event}
    private static final Pattern PATTERN = Pattern.compile("v4/([a-z_]+)/([a-z_]+)");

    @Override
    public Pattern getPattern() {
        return PATTERN;
    }

    @Override
    protected String getWorkspaceId(Matcher matcher) {
        return null;
    }

    @Override
    protected String getResourceName(Matcher matcher) {
        return null;
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
        return matcher.group(RESOURCE_TYPE_GROUP_NUMBER);
    }

    @Override
    protected String getResourceEvent(Matcher matcher) {
        return matcher.group(RESOURCE_EVENT_GROUP_NUMBER);
    }

}
