package com.sequenceiq.cloudbreak.cloud.mock;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.NoSqlConnector;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Variant;
import com.sequenceiq.cloudbreak.cloud.model.base.ResponseStatus;
import com.sequenceiq.cloudbreak.cloud.model.nosql.NoSqlTableDeleteRequest;
import com.sequenceiq.cloudbreak.cloud.model.nosql.NoSqlTableDeleteResponse;
import com.sequenceiq.cloudbreak.cloud.model.nosql.NoSqlTableMetadataRequest;
import com.sequenceiq.cloudbreak.cloud.model.nosql.NoSqlTableMetadataResponse;

@Service
public class MockNoSqlConnector implements NoSqlConnector {

    @Override
    public NoSqlTableMetadataResponse getNoSqlTableMetaData(NoSqlTableMetadataRequest request) {
        return NoSqlTableMetadataResponse.builder().withId("id").withStatus(ResponseStatus.OK).withTableStatus("ACTIVE").build();
    }

    @Override
    public NoSqlTableDeleteResponse deleteNoSqlTable(NoSqlTableDeleteRequest request) {
        return NoSqlTableDeleteResponse.builder().build();
    }

    @Override
    public Platform platform() {
        return Platform.platform("MOCK");
    }

    @Override
    public Variant variant() {
        return Variant.variant("MOCK");
    }

}
