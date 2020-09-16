package com.sequenceiq.environment.events;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakEventService;
import com.sequenceiq.cloudbreak.structuredevent.rest.urlparser.CDPRestUrlParser;

@Component
public class EnvironmentUrlParser extends CDPRestUrlParser {

    private static final String NAME = "name";

    private static final String CRN = "crn";

    private static final String CRN_BY_NAME = "crnByName";

    private static final Pattern PATTERN = Pattern.compile("v1/env(/((?:" + NAME + '|' + CRN_BY_NAME + '|' + CRN + "))/?([^/]+)?/?(.*))?");

    private static final int MIN_GROUP_COUNT_FOR_ID = 2;

    private static final int ID_GROUP_NUMBER = 3;

    private static final int MIN_GROUP_COUNT_FOR_EVENT = 3;

    private static final int EVENT_GROUP_NUMBER_WITH_RESOURCE = 4;

    private static final int EVENT_GROUP_NUMBER = 3;

    @Override
    protected Pattern getPattern() {
        return PATTERN;
    }

    @Override
    protected String getResourceName(Matcher matcher) {
        if (matcher.groupCount() > MIN_GROUP_COUNT_FOR_ID
                && (NAME.equals(matcher.group(MIN_GROUP_COUNT_FOR_ID)) || CRN_BY_NAME.equals(matcher.group(MIN_GROUP_COUNT_FOR_ID)))) {
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
        return CloudbreakEventService.ENVIRONMENT_RESOURCE_TYPE;
    }

    @Override
    protected String getResourceEvent(Matcher matcher) {
        if (matcher.groupCount() > MIN_GROUP_COUNT_FOR_EVENT) {
            return getResourceIdentifier(matcher);
        }
        return null;
    }

    @Override
    protected String getIdType(Matcher matcher) {
        return matcher.group(2);
    }

    private String getResourceIdentifier(Matcher matcher) {
        String eventWithResource = matcher.group(EVENT_GROUP_NUMBER_WITH_RESOURCE);
        if (StringUtils.isEmpty(eventWithResource)) {
            eventWithResource = matcher.group(EVENT_GROUP_NUMBER);
        }
        return eventWithResource;
    }

}
