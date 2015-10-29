package com.sequenceiq.cloudbreak.cloud.handler;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.common.collect.Maps;
import com.sequenceiq.cloudbreak.cloud.event.platform.GetPlatformRegionsRequest;
import com.sequenceiq.cloudbreak.cloud.event.platform.GetPlatformRegionsResult;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.PlatformRegions;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.cloud.model.Variant;

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
            Map<Platform, Collection<Region>> platformRegions = Maps.newHashMap();
            Map<Platform, Map<Region, List<AvailabilityZone>>> platformAvailabilityZones = Maps.newHashMap();
            Map<Platform, Region> platformDefaultRegion = Maps.newHashMap();
            for (Map.Entry<Platform, Collection<Variant>> connector : cloudPlatformConnectors.getPlatformVariants().getPlatformToVariants().entrySet()) {
                Region defaultRegion = cloudPlatformConnectors.getDefault(connector.getKey()).parameters().regions().defaultType();
                Collection<Region> regions = cloudPlatformConnectors.getDefault(connector.getKey()).parameters().regions().types();
                Map<Region, List<AvailabilityZone>> availabilityZones = cloudPlatformConnectors.getDefault(connector.getKey()).parameters()
                        .availabilityZones().getAll();
                platformAvailabilityZones.put(connector.getKey(), availabilityZones);
                platformRegions.put(connector.getKey(), regions);
                platformDefaultRegion.put(connector.getKey(), defaultRegion);
            }
            PlatformRegions pv = new PlatformRegions(platformRegions, platformAvailabilityZones, platformDefaultRegion);
            GetPlatformRegionsResult getPlatformRegionsResult = new GetPlatformRegionsResult(request, pv);
            request.getResult().onNext(getPlatformRegionsResult);
            LOGGER.info("Query platform machine types types finished.");
        } catch (Exception e) {
            request.getResult().onNext(new GetPlatformRegionsResult(e.getMessage(), e, request));
        }
    }
}
