package com.sequenceiq.cloudbreak.cloud.handler;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.event.platform.GetPlatformNoSqlTablesRequest;
import com.sequenceiq.cloudbreak.cloud.event.platform.GetPlatformNoSqlTablesResult;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudPlatformVariant;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.cloud.model.Variant;
import com.sequenceiq.cloudbreak.cloud.model.nosql.CloudNoSqlTables;

import reactor.bus.Event;

@Component
public class GetPlatformNoSqlTablesHandler implements CloudPlatformEventHandler<GetPlatformNoSqlTablesRequest> {
    private static final Logger LOGGER = LoggerFactory.getLogger(GetPlatformNoSqlTablesHandler.class);

    @Inject
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Override
    public Class<GetPlatformNoSqlTablesRequest> type() {
        return GetPlatformNoSqlTablesRequest.class;
    }

    @Override
    public void accept(Event<GetPlatformNoSqlTablesRequest> event) {
        LOGGER.debug("Received event: {}", event);
        GetPlatformNoSqlTablesRequest request = event.getData();

        try {
            CloudPlatformVariant cloudPlatformVariant = new CloudPlatformVariant(
                    Platform.platform(request.getExtendedCloudCredential().getCloudPlatform()),
                    Variant.variant(request.getVariant()));
            CloudNoSqlTables cloudNoSqlTables = cloudPlatformConnectors.get(cloudPlatformVariant)
                    .platformResources().noSqlTables(request.getCloudCredential(), Region.region(request.getRegion()), request.getFilters());
            GetPlatformNoSqlTablesResult result = new GetPlatformNoSqlTablesResult(request.getResourceId(), cloudNoSqlTables);
            request.getResult().onNext(result);
            LOGGER.debug("Query platform NoSQL tables finished.");
        } catch (RuntimeException e) {
            request.getResult().onNext(new GetPlatformNoSqlTablesResult(e.getMessage(), e, request.getResourceId()));
        }
    }
}
