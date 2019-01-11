package com.sequenceiq.cloudbreak.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.SupportedDatabaseEntryV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.SupportedExternalDatabaseServiceEntryV4Response;
import com.sequenceiq.cloudbreak.validation.externaldatabase.SupportedExternalDatabaseServiceEntry;

@Component
public class SupportedExternalDatabaseServiceEntryToSupportedExternalDatabaseServiceEntryResponseConverter
        extends AbstractConversionServiceAwareConverter<SupportedExternalDatabaseServiceEntry, SupportedExternalDatabaseServiceEntryV4Response> {

    @Override
    public SupportedExternalDatabaseServiceEntryV4Response convert(SupportedExternalDatabaseServiceEntry source) {
        SupportedExternalDatabaseServiceEntryV4Response supportedExternalDatabaseServiceEntryV4Response = new SupportedExternalDatabaseServiceEntryV4Response();
        supportedExternalDatabaseServiceEntryV4Response.setDisplayName(source.getDisplayName());
        supportedExternalDatabaseServiceEntryV4Response.setName(source.getName());

        source.getDatabases().forEach(item ->
                supportedExternalDatabaseServiceEntryV4Response.getDatabases()
                        .add(getConversionService().convert(item, SupportedDatabaseEntryV4Response.class)));
        return supportedExternalDatabaseServiceEntryV4Response;
    }
}
