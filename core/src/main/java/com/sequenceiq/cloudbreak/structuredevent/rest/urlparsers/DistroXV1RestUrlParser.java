package com.sequenceiq.cloudbreak.structuredevent.rest.urlparsers;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

@Component
public class DistroXV1RestUrlParser extends RestUrlParser {

    private static final String NAME = "name";

    private static final String CRN = "crn";

    private static final Pattern PATTERN = Pattern.compile("v1/distrox(/((?:" + NAME + '|' + CRN + "))/([^/]+)/?(.*))?");

    private static final int MIN_GROUP_COUNT_FOR_ID = 2;

    private static final int ID_GROUP_NUMBER = 3;

    private static final int MIN_GROUP_COUNT_FOR_EVENT = 3;

    private static final int EVENT_GROUP_NUMBER = 4;

    @Override
    protected Pattern getPattern() {
        return PATTERN;
    }

    @Override
    protected String getWorkspaceId(Matcher matcher) {
        return null;
    }

    @Override
    protected String getResourceName(Matcher matcher) {
        if (matcher.groupCount() > MIN_GROUP_COUNT_FOR_ID && NAME.equals(matcher.group(MIN_GROUP_COUNT_FOR_ID))) {
            return matcher.group(ID_GROUP_NUMBER);
        }
        return null;
    }

    @Override
    protected String getResourceCrn(Matcher matcher) {
        if (matcher.groupCount() > MIN_GROUP_COUNT_FOR_ID && CRN.equals(matcher.group(MIN_GROUP_COUNT_FOR_ID))) {
            return matcher.group(ID_GROUP_NUMBER);
        }
        return null;
    }

    @Override
    protected String getResourceId(Matcher matcher) {
        return null;
    }

    @Override
    protected String getResourceType(Matcher matcher) {
        return "stacks";
    }

    @Override
    protected String getResourceEvent(Matcher matcher) {
        if (matcher.groupCount() > MIN_GROUP_COUNT_FOR_EVENT) {
            return matcher.group(EVENT_GROUP_NUMBER);
        }
        return null;
    }
}
