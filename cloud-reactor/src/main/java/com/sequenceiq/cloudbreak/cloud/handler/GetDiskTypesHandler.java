package com.sequenceiq.cloudbreak.cloud.handler;

import java.util.Collection;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.common.collect.Maps;
import com.sequenceiq.cloudbreak.cloud.event.platform.GetDiskTypesRequest;
import com.sequenceiq.cloudbreak.cloud.event.platform.GetDiskTypesResult;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.DiskType;
import com.sequenceiq.cloudbreak.cloud.model.DiskTypes;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.PlatformDisks;
import com.sequenceiq.cloudbreak.cloud.model.Variant;
import com.sequenceiq.cloudbreak.cloud.model.VolumeParameterType;

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
            Map<Platform, Collection<DiskType>> platformDiskTypes = Maps.newHashMap();
            Map<Platform, DiskType> defaultDiskTypes = Maps.newHashMap();
            Map<Platform, Map<String, VolumeParameterType>> diskMappings = Maps.newHashMap();


            for (Map.Entry<Platform, Collection<Variant>> connector : cloudPlatformConnectors.getPlatformVariants().getPlatformToVariants().entrySet()) {
                DiskTypes diskTypes = cloudPlatformConnectors.getDefault(connector.getKey()).parameters().diskTypes();
                defaultDiskTypes.put(connector.getKey(), diskTypes.defaultType());
                platformDiskTypes.put(connector.getKey(), diskTypes.types());
                diskMappings.put(connector.getKey(), diskTypes.diskMapping());
            }
            GetDiskTypesResult getDiskTypesResult = new GetDiskTypesResult(request, new PlatformDisks(platformDiskTypes, defaultDiskTypes, diskMappings));
            request.getResult().onNext(getDiskTypesResult);
            LOGGER.info("Query platform disk types finished.");
        } catch (Exception e) {
            request.getResult().onNext(new GetDiskTypesResult(e.getMessage(), e, request));
        }
    }
}
