package com.sequenceiq.environment.platformresource.v1.converter;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.nosql.CloudNoSqlTables;
import com.sequenceiq.environment.api.v1.platformresource.model.PlatformNoSqlTableResponse;
import com.sequenceiq.environment.api.v1.platformresource.model.PlatformNoSqlTablesResponse;

@Component
public class CloudNoSqlTablesToPlatformNoSqlTablesV1ResponseConverter {

    public PlatformNoSqlTablesResponse convert(CloudNoSqlTables source) {
        List<PlatformNoSqlTableResponse> result = source.getCloudNoSqlTables()
                .stream()
                .map(t -> new PlatformNoSqlTableResponse(t.getName()))
                .collect(Collectors.toList());
        return new PlatformNoSqlTablesResponse(result);
    }
}
