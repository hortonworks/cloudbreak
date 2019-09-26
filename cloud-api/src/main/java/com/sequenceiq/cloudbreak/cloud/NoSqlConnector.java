package com.sequenceiq.cloudbreak.cloud;

import com.sequenceiq.cloudbreak.cloud.model.nosql.NoSqlTableDeleteRequest;
import com.sequenceiq.cloudbreak.cloud.model.nosql.NoSqlTableDeleteResponse;
import com.sequenceiq.cloudbreak.cloud.model.nosql.NoSqlTableMetadataRequest;
import com.sequenceiq.cloudbreak.cloud.model.nosql.NoSqlTableMetadataResponse;

/**
 * NoSQL storage connector
 */
public interface NoSqlConnector extends CloudPlatformAware {

    NoSqlTableMetadataResponse getNoSqlTableMetaData(NoSqlTableMetadataRequest request);

    NoSqlTableDeleteResponse deleteNoSqlTable(NoSqlTableDeleteRequest request);

}
