package com.sequenceiq.redbeams.converter.v4.databaseserver;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.DatabaseServerAllocationOutcomeV4Response;
import com.sequenceiq.redbeams.domain.stack.DBStack;

@Component
public class DBStackToDatabaseServerAllocationOutcomeV4ResponseConverter
    extends AbstractConversionServiceAwareConverter<DBStack, DatabaseServerAllocationOutcomeV4Response> {

    @Override
    public DatabaseServerAllocationOutcomeV4Response convert(DBStack source) {
        DatabaseServerAllocationOutcomeV4Response response = new DatabaseServerAllocationOutcomeV4Response();
        response.setName(source.getName());
        response.setEnvironmentId(source.getEnvironmentId());

        response.setStatus(source.getStatus());
        response.setStatusReason(source.getStatusReason());

        return response;
    }
}
