package com.sequenceiq.cloudbreak.cloud.handler;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.event.platform.GetPlatformRegionsRequest;
import com.sequenceiq.cloudbreak.cloud.event.platform.GetPlatformRegionsResult;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.PlatformRegions;

import reactor.bus.Event;

@Component
public class GetRegionsHandler implements CloudPlatformEventHandler<GetPlatformRegionsRequest> {
    private static final Logger LOGGER = LoggerFactory.getLogger(GetRegionsHandler.class);

    @Inject
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Override
    public Class<GetPlatformRegionsRequest> type() {
        return GetPlatformRegionsRequest.class;
    }

    @Override
    public void accept(Event<GetPlatformRegionsRequest> getRegionsRequestEvent) {
        LOGGER.info("Received event: {}", getRegionsRequestEvent);
        GetPlatformRegionsRequest request = getRegionsRequestEvent.getData();
        try {
            PlatformRegions pv = new PlatformRegions();
            for (Map.Entry<String, Collection<String>> connector : cloudPlatformConnectors.getPlatformVariants().getPlatformToVariants().entrySet()) {
                String region = cloudPlatformConnectors.getDefault(connector.getKey()).parameters().defaultRegion();
                Map<String, String> stringStringMap = cloudPlatformConnectors.getDefault(connector.getKey()).parameters().regions();
                Map<String, List<String>> stringMapMap = cloudPlatformConnectors.getDefault(connector.getKey()).parameters().availabiltyZones();
                pv.getAvailabiltyZones().put(connector.getKey(), stringMapMap);
                pv.getRegions().put(connector.getKey(), stringStringMap);
                pv.getDefaultRegions().put(connector.getKey(), region);
            }
            GetPlatformRegionsResult getPlatformRegionsResult = new GetPlatformRegionsResult(request, pv);
            request.getResult().onNext(getPlatformRegionsResult);
            LOGGER.info("Query platform machine types types finished.");
        } catch (Exception e) {
            request.getResult().onNext(new GetPlatformRegionsResult(e.getMessage(), e, request));
        }
    }
}
