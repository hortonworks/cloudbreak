package com.sequenceiq.cloudbreak.structuredevent.rest.urlparsers;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

@Component
public class FlowRestUrlParser extends RestUrlParser {

    public static final int RESOURCE_CRN_GROUP_NUMBER = 7;

    public static final int RESOURCE_NAME_GROUP_NUMBER = 4;

    public static final int FLOW_ID_GROUP_NUMBER = 8;

    private static final Pattern PATTERN = Pattern.compile("flow_logs/(resource/((name/([^\\/]+)(/([^\\/]+))?|crn/([^/]+)))|([^/]+))(/last)?");

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
        return StringUtils.isNotBlank(matcher.group(RESOURCE_NAME_GROUP_NUMBER)) ? matcher.group(RESOURCE_NAME_GROUP_NUMBER) :
                StringUtils.isNotBlank(matcher.group(FLOW_ID_GROUP_NUMBER)) ? matcher.group(FLOW_ID_GROUP_NUMBER) : matcher.group(RESOURCE_CRN_GROUP_NUMBER);
    }

    @Override
    protected String getResourceCrn(Matcher matcher) {
        return StringUtils.isNotBlank(matcher.group(RESOURCE_CRN_GROUP_NUMBER)) ? matcher.group(RESOURCE_CRN_GROUP_NUMBER) : null;
    }

    @Override
    protected String getResourceId(Matcher matcher) {
        return null;
    }

    @Override
    protected String getResourceType(Matcher matcher) {
        return "flow_logs";
    }

    @Override
    protected String getResourceEvent(Matcher matcher) {
        return null;
    }

}
