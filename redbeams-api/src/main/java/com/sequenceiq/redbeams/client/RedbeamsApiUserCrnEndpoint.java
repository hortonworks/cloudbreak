package com.sequenceiq.redbeams.client;

import javax.ws.rs.client.WebTarget;

import com.sequenceiq.cloudbreak.client.AbstractUserCrnServiceEndpoint;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.DatabaseServerV4Endpoint;

public class RedbeamsApiUserCrnEndpoint extends AbstractUserCrnServiceEndpoint implements RedbeamsClient {
    protected RedbeamsApiUserCrnEndpoint(WebTarget webTarget, String crn) {
        super(webTarget, crn);
    }

    public DatabaseServerV4Endpoint getDatabaseServerV4Endpoint() {
        return getEndpoint(DatabaseServerV4Endpoint.class);
    }
}
