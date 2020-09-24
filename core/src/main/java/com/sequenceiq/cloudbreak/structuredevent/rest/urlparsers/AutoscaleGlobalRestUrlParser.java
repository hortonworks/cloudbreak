package com.sequenceiq.cloudbreak.structuredevent.rest.urlparsers;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.structuredevent.rest.urlparser.LegacyRestUrlParser;

@Component
public class AutoscaleGlobalRestUrlParser extends LegacyRestUrlParser {

    public static final int RESOURCE_EVENT_GROUP_NUMBER = 1;

    private static final Pattern PATTERN = Pattern.compile("autoscale/(ambari|stack/all)");

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
        return "autoscale";
    }

    @Override
    protected String getResourceEvent(Matcher matcher) {
        return matcher.group(RESOURCE_EVENT_GROUP_NUMBER);
    }

}
