package com.sequenceiq.cloudbreak.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.SupportedDatabaseEntryResponse;
import com.sequenceiq.cloudbreak.cloud.model.SupportedDatabaseEntry;


@Component
public class SupportedDatabasEntryToSupportedDatabasEntryResponseConverter
        extends AbstractConversionServiceAwareConverter<SupportedDatabaseEntry, SupportedDatabaseEntryResponse> {

    @Override
    public SupportedDatabaseEntryResponse convert(SupportedDatabaseEntry source) {
        SupportedDatabaseEntryResponse supportedDatabaseEntryResponse = new SupportedDatabaseEntryResponse();
        supportedDatabaseEntryResponse.setDisplayName(source.getDisplayName());
        supportedDatabaseEntryResponse.setDatabaseName(source.getDatabaseName());
        supportedDatabaseEntryResponse.setJdbcPrefix(source.getJdbcPrefix());
        return supportedDatabaseEntryResponse;
    }
}
