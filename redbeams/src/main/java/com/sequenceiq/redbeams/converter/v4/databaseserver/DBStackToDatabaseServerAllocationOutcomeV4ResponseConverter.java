package com.sequenceiq.redbeams.converter.v4.databaseserver;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.DatabaseServerStatusV4Response;
import com.sequenceiq.redbeams.domain.stack.DBStack;

@Component
public class DBStackToDatabaseServerAllocationOutcomeV4ResponseConverter
    extends AbstractConversionServiceAwareConverter<DBStack, DatabaseServerStatusV4Response> {

    @Override
    public DatabaseServerStatusV4Response convert(DBStack source) {
        DatabaseServerStatusV4Response response = new DatabaseServerStatusV4Response();
        response.setName(source.getName());
        response.setEnvironmentId(source.getEnvironmentId());

        response.setStatus(source.getStatus());
        response.setStatusReason(source.getStatusReason());

        return response;
    }
}
