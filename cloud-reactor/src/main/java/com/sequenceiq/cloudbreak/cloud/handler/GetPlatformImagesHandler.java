package com.sequenceiq.cloudbreak.cloud.handler;

import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.common.collect.Maps;
import com.sequenceiq.cloudbreak.cloud.event.platform.GetPlatformImagesRequest;
import com.sequenceiq.cloudbreak.cloud.event.platform.GetPlatformImagesResult;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CustomImage;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.PlatformImage;
import com.sequenceiq.cloudbreak.cloud.model.PlatformImages;
import com.sequenceiq.cloudbreak.cloud.model.Variant;

import reactor.bus.Event;

@Component
public class GetPlatformImagesHandler implements CloudPlatformEventHandler<GetPlatformImagesRequest> {
    private static final Logger LOGGER = LoggerFactory.getLogger(GetPlatformImagesHandler.class);

    @Inject
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Override
    public Class<GetPlatformImagesRequest> type() {
        return GetPlatformImagesRequest.class;
    }

    @Override
    public void accept(Event<GetPlatformImagesRequest> getPlatformImagesRequestEvent) {
        LOGGER.info("Received event: {}", getPlatformImagesRequestEvent);
        GetPlatformImagesRequest request = getPlatformImagesRequestEvent.getData();
        try {
            Map<Platform, Collection<CustomImage>> platformCollectionHashMap = Maps.newHashMap();
            Map<Platform, String> platformRegexCollectionHashMap = Maps.newHashMap();

            for (Entry<Platform, Collection<Variant>> connector : cloudPlatformConnectors.getPlatformVariants().getPlatformToVariants().entrySet()) {
                PlatformImage platformImage = cloudPlatformConnectors.getDefault(connector.getKey()).parameters().images();

                platformCollectionHashMap.put(connector.getKey(), platformImage.types());
                platformRegexCollectionHashMap.put(connector.getKey(), platformImage.getRegex());
            }
            GetPlatformImagesResult getPlatformImagesResult = new GetPlatformImagesResult(request,
                    new PlatformImages(platformCollectionHashMap, platformRegexCollectionHashMap));
            request.getResult().onNext(getPlatformImagesResult);
            LOGGER.info("Query platform images types finished.");
        } catch (RuntimeException e) {
            request.getResult().onNext(new GetPlatformImagesResult(e.getMessage(), e, request));
        }
    }
}
