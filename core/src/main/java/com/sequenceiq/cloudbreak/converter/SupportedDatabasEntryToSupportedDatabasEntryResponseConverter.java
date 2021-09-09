package com.sequenceiq.cloudbreak.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.SupportedDatabaseEntryV4Response;
import com.sequenceiq.cloudbreak.validation.externaldatabase.SupportedDatabaseEntry;

@Component
public class SupportedDatabasEntryToSupportedDatabasEntryResponseConverter {

    public SupportedDatabaseEntryV4Response convert(SupportedDatabaseEntry source) {
        SupportedDatabaseEntryV4Response supportedDatabaseEntryV4Response = new SupportedDatabaseEntryV4Response();
        supportedDatabaseEntryV4Response.setDisplayName(source.getDisplayName());
        supportedDatabaseEntryV4Response.setDatabaseName(source.getDatabaseName());
        supportedDatabaseEntryV4Response.setJdbcPrefix(source.getJdbcPrefix());
        supportedDatabaseEntryV4Response.setVersions(source.getVersions());
        return supportedDatabaseEntryV4Response;
    }
}
