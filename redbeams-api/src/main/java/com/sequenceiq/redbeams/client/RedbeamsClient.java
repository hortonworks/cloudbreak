package com.sequenceiq.redbeams.client;

import com.sequenceiq.redbeams.api.endpoint.v4.database.DatabaseV4Endpoint;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.DatabaseServerV4Endpoint;

public interface RedbeamsClient {

    DatabaseV4Endpoint databaseV4Endpoint();

    DatabaseServerV4Endpoint databaseServerV4Endpoint();
}
