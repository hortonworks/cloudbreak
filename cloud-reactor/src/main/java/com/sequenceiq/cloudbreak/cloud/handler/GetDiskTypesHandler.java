package com.sequenceiq.cloudbreak.cloud.handler;

import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.common.collect.Maps;
import com.sequenceiq.cloudbreak.cloud.event.platform.GetDiskTypesRequest;
import com.sequenceiq.cloudbreak.cloud.event.platform.GetDiskTypesResult;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.DiskType;
import com.sequenceiq.cloudbreak.cloud.model.DiskTypes;
import com.sequenceiq.cloudbreak.cloud.model.DisplayName;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.PlatformDisks;
import com.sequenceiq.cloudbreak.cloud.model.Variant;
import com.sequenceiq.cloudbreak.cloud.model.VolumeParameterType;
import com.sequenceiq.cloudbreak.eventbus.Event;

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
        LOGGER.debug("Received event: {}", getDiskTypesRequestEvent);
        GetDiskTypesRequest request = getDiskTypesRequestEvent.getData();
        try {
            Map<Platform, Collection<DiskType>> platformDiskTypes = Maps.newHashMap();
            Map<Platform, DiskType> defaultDiskTypes = Maps.newHashMap();
            Map<Platform, Map<String, VolumeParameterType>> diskMappings = Maps.newHashMap();
            Map<Platform, Map<DiskType, DisplayName>> diskDisplayNames = Maps.newHashMap();

            for (Entry<Platform, Collection<Variant>> connector : cloudPlatformConnectors.getPlatformVariants().getPlatformToVariants().entrySet()) {
                DiskTypes diskTypes = cloudPlatformConnectors.getDefault(connector.getKey()).parameters().diskTypes();
                defaultDiskTypes.put(connector.getKey(), diskTypes.defaultType());
                platformDiskTypes.put(connector.getKey(), diskTypes.types());
                diskMappings.put(connector.getKey(), diskTypes.diskMapping());
                diskDisplayNames.put(connector.getKey(), diskTypes.displayNames());
            }
            GetDiskTypesResult getDiskTypesResult = new GetDiskTypesResult(request.getResourceId(),
                    new PlatformDisks(platformDiskTypes, defaultDiskTypes, diskMappings, diskDisplayNames));
            request.getResult().onNext(getDiskTypesResult);
            LOGGER.debug("Query platform disk types finished.");
        } catch (RuntimeException e) {
            request.getResult().onNext(new GetDiskTypesResult(e.getMessage(), e, request.getResourceId()));
        }
    }
}
