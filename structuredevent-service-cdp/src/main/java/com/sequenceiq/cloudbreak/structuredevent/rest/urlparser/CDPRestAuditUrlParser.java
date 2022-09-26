package com.sequenceiq.cloudbreak.structuredevent.rest.urlparser;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class CDPRestAuditUrlParser extends CDPRestUrlParser {
    private static final String NAME = "name";

    private static final String CRN = "crn";

    private static final String CRN_BY_NAME = "crnByName";

    private static final String PATTERN_WITHOUT_CONTEXT_PATH = "^%s(v[0-9]+){0,1}((.*)/(name|crn|crnByName)(/[^/]+)/?(.*))?$";

    private static final int MIN_GROUP_COUNT_FOR_ID = 4;

    private static final int ID_GROUP_NUMBER = 5;

    private static final int MIN_GROUP_COUNT_FOR_EVENT = 5;

    private static final int EVENT_GROUP_NUMBER_WITH_RESOURCE = 6;

    private final Pattern auditPattern;

    public CDPRestAuditUrlParser(@Value("${server.servlet.context-path:}") String contextPath) {
        if (!contextPath.endsWith("/")) {
            contextPath += "/";
        }
        contextPath += "api/";
        String patternWithContextPath = String.format(PATTERN_WITHOUT_CONTEXT_PATH, contextPath);
        this.auditPattern = Pattern.compile(patternWithContextPath);
    }

    @Override
    protected Pattern getPattern() {
        return auditPattern;
    }

    @Override
    protected String getResourceName(Matcher matcher) {
        if (matcher.groupCount() > MIN_GROUP_COUNT_FOR_ID
                && (NAME.equals(matcher.group(MIN_GROUP_COUNT_FOR_ID)) || CRN_BY_NAME.equals(matcher.group(MIN_GROUP_COUNT_FOR_ID)))) {
            return matcher.group(ID_GROUP_NUMBER).replaceAll("/", "");
        }
        return null;
    }

    @Override
    protected String getResourceCrn(Matcher matcher) {
        if (matcher.groupCount() > MIN_GROUP_COUNT_FOR_ID && CRN.equals(matcher.group(MIN_GROUP_COUNT_FOR_ID))) {
            return matcher.group(ID_GROUP_NUMBER).replaceAll("/", "");
        }
        return null;
    }

    @Override
    protected String getResourceId(Matcher matcher) {
        return null;
    }

    @Override
    protected String getResourceType(Matcher matcher) {
        return null;
    }

    @Override
    protected String getResourceEvent(Matcher matcher) {
        if (matcher.groupCount() > MIN_GROUP_COUNT_FOR_EVENT) {
            String event = matcher.group(EVENT_GROUP_NUMBER_WITH_RESOURCE);
            return StringUtils.isNotEmpty(event) ? event : null;
        }
        return null;
    }

    @Override
    protected String getIdType(Matcher matcher) {
        return matcher.group(2);
    }
}
