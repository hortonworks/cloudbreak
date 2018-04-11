package com.sequenceiq.cloudbreak.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.SupportedDatabaseEntryResponse;
import com.sequenceiq.cloudbreak.api.model.SupportedExternalDatabaseServiceEntryResponse;
import com.sequenceiq.cloudbreak.validation.externaldatabase.SupportedExternalDatabaseServiceEntry;


@Component
public class SupportedExternalDatabaseServiceEntryToSupportedExternalDatabaseServiceEntryResponseConverter
        extends AbstractConversionServiceAwareConverter<SupportedExternalDatabaseServiceEntry, SupportedExternalDatabaseServiceEntryResponse> {

    @Override
    public SupportedExternalDatabaseServiceEntryResponse convert(SupportedExternalDatabaseServiceEntry source) {
        SupportedExternalDatabaseServiceEntryResponse supportedExternalDatabaseServiceEntryResponse = new SupportedExternalDatabaseServiceEntryResponse();
        supportedExternalDatabaseServiceEntryResponse.setDisplayName(source.getDisplayName());
        supportedExternalDatabaseServiceEntryResponse.setName(source.getName());

        source.getDatabases().forEach(item ->
                supportedExternalDatabaseServiceEntryResponse.getDatabases()
                        .add(getConversionService().convert(item, SupportedDatabaseEntryResponse.class)));
        return supportedExternalDatabaseServiceEntryResponse;
    }
}
