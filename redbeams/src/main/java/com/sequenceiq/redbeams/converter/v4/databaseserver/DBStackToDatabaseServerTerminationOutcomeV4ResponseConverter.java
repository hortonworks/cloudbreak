package com.sequenceiq.redbeams.converter.v4.databaseserver;

import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.DatabaseServerTerminationOutcomeV4Response;
import com.sequenceiq.redbeams.domain.stack.DBStack;

import org.springframework.stereotype.Component;

@Component
public class DBStackToDatabaseServerTerminationOutcomeV4ResponseConverter
    extends AbstractConversionServiceAwareConverter<DBStack, DatabaseServerTerminationOutcomeV4Response> {

    @Override
    public DatabaseServerTerminationOutcomeV4Response convert(DBStack source) {
        DatabaseServerTerminationOutcomeV4Response response = new DatabaseServerTerminationOutcomeV4Response();
        response.setName(source.getName());
        response.setEnvironmentId(source.getEnvironmentId());

        response.setStatus(source.getStatus());
        response.setStatusReason(source.getStatusReason());

        return response;
    }
}
