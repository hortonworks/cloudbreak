package com.sequenceiq.cloudbreak.converter;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.SupportedExternalDatabaseServiceEntryV4Response;
import com.sequenceiq.cloudbreak.validation.externaldatabase.SupportedExternalDatabaseServiceEntry;

@Component
public class SupportedExternalDatabaseServiceEntryToSupportedExternalDatabaseServiceEntryResponseConverter {

    @Inject
    private SupportedDatabasEntryToSupportedDatabasEntryResponseConverter supportedDatabasEntryToSupportedDatabasEntryResponseConverter;

    public SupportedExternalDatabaseServiceEntryV4Response convert(SupportedExternalDatabaseServiceEntry source) {
        SupportedExternalDatabaseServiceEntryV4Response supportedExternalDatabaseServiceEntryV4Response = new SupportedExternalDatabaseServiceEntryV4Response();
        supportedExternalDatabaseServiceEntryV4Response.setDisplayName(source.getDisplayName());
        supportedExternalDatabaseServiceEntryV4Response.setName(source.getName());

        source.getDatabases().forEach(item ->
                supportedExternalDatabaseServiceEntryV4Response.getDatabases()
                        .add(supportedDatabasEntryToSupportedDatabasEntryResponseConverter
                                .convert(item)));
        return supportedExternalDatabaseServiceEntryV4Response;
    }
}
