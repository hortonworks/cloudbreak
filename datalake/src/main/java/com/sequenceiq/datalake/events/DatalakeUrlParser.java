package com.sequenceiq.datalake.events;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.structuredevent.rest.urlparser.CDPRestUrlParser;

// todo: https://cloudera.atlassian.net/browse/CB-13786 consider renaming to BackupRestoreParser
@Component
public class DatalakeUrlParser extends CDPRestUrlParser {
    // todo: https://cloudera.atlassian.net/browse/CB-13786 implement me
    @Override
    protected Pattern getPattern() {
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
        return null;
    }

    @Override
    protected String getResourceEvent(Matcher matcher) {
        return null;
    }

    @Override
    protected String getIdType(Matcher matcher) {
        return null;
    }
}
