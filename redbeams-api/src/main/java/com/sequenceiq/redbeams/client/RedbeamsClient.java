package com.sequenceiq.redbeams.client;

import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.DatabaseServerV4Endpoint;

public interface RedbeamsClient {

    DatabaseServerV4Endpoint getDatabaseServerV4Endpoint();
}
