package com.sequenceiq.redbeams.client;

import javax.ws.rs.client.WebTarget;

import com.sequenceiq.cloudbreak.client.AbstractKeyBasedServiceEndpoint;
import com.sequenceiq.redbeams.api.endpoint.v4.database.DatabaseV4Endpoint;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.DatabaseServerV4Endpoint;

public class RedbeamsApiKeyEndpoints extends AbstractKeyBasedServiceEndpoint implements RedbeamsClient {

    protected RedbeamsApiKeyEndpoints(WebTarget webTarget, String accessKey, String secretKey) {
        super(webTarget, accessKey, secretKey);
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

