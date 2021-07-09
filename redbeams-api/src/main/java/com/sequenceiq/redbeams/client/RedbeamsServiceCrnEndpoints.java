package com.sequenceiq.redbeams.client;

import javax.ws.rs.client.WebTarget;

import com.sequenceiq.cloudbreak.client.AbstractUserCrnServiceEndpoint;
import com.sequenceiq.redbeams.api.endpoint.v4.database.DatabaseV4Endpoint;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.DatabaseServerV4Endpoint;
import com.sequenceiq.redbeams.api.endpoint.v4.operation.OperationV4Endpoint;
import com.sequenceiq.redbeams.api.endpoint.v4.progress.ProgressV4Endpoint;

public class RedbeamsServiceCrnEndpoints extends AbstractUserCrnServiceEndpoint implements RedbeamsClient {

    protected RedbeamsServiceCrnEndpoints(WebTarget webTarget, String crn) {
        super(webTarget, crn);
    }

    @Override
    public DatabaseV4Endpoint databaseV4Endpoint() {
        return getEndpoint(DatabaseV4Endpoint.class);
    }

    @Override
    public DatabaseServerV4Endpoint databaseServerV4Endpoint() {
        return getEndpoint(DatabaseServerV4Endpoint.class);
    }

    @Override
    public ProgressV4Endpoint progressV4Endpoint() {
        return getEndpoint(ProgressV4Endpoint.class);
    }

    @Override
    public OperationV4Endpoint operationV4Endpoint() {
        return getEndpoint(OperationV4Endpoint.class);
    }
}
