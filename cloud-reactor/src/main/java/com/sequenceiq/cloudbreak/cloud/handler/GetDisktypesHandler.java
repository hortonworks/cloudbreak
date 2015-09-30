package com.sequenceiq.cloudbreak.cloud.handler;

import java.util.Collection;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.event.platform.GetDiskTypesRequest;
import com.sequenceiq.cloudbreak.cloud.event.platform.GetDiskTypesResult;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.PlatformDisks;

import reactor.bus.Event;

@Component
public class GetDiskTypesHandler implements CloudPlatformEventHandler<GetDiskTypesRequest> {
    private static final Logger LOGGER = LoggerFactory.getLogger(GetDiskTypesHandler.class);

    @Inject
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Override
    public Class<GetDiskTypesRequest> type() {
        return GetDiskTypesRequest.class;
    }

    @Override
    public void accept(Event<GetDiskTypesRequest> getDiskTypesRequestEvent) {
        LOGGER.info("Received event: {}", getDiskTypesRequestEvent);
        GetDiskTypesRequest request = getDiskTypesRequestEvent.getData();
        try {
            PlatformDisks pv = new PlatformDisks();
            for (Map.Entry<String, Collection<String>> connector : cloudPlatformConnectors.getPlatformVariants().getPlatformToVariants().entrySet()) {
                String defaultDiskType = cloudPlatformConnectors.getDefault(connector.getKey()).parameters().defaultDiskType();
                Map<String, String> stringStringMap = cloudPlatformConnectors.getDefault(connector.getKey()).parameters().diskTypes();
                pv.getDefaultDisks().put(connector.getKey(), defaultDiskType);
                pv.getDiskTypes().put(connector.getKey(), stringStringMap);
            }
            GetDiskTypesResult getDiskTypesResult = new GetDiskTypesResult(request, pv);
            request.getResult().onNext(getDiskTypesResult);
            LOGGER.info("Query platform disk types finished.");
        } catch (Exception e) {
            request.getResult().onNext(new GetDiskTypesResult(e.getMessage(), e, request));
        }
    }
}
