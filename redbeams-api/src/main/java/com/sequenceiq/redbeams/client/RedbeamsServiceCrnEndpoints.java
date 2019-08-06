package com.sequenceiq.redbeams.client;

import javax.ws.rs.client.WebTarget;

import com.sequenceiq.cloudbreak.client.AbstractUserCrnServiceEndpoint;
import com.sequenceiq.redbeams.api.endpoint.v4.database.DatabaseV4Endpoint;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.DatabaseServerV4Endpoint;

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
}
