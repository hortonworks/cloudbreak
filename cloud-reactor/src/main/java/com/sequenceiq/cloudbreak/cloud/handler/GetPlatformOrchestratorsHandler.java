package com.sequenceiq.cloudbreak.cloud.handler;

import java.util.Collection;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.common.collect.Maps;
import com.sequenceiq.cloudbreak.cloud.event.platform.GetPlatformOrchestratorsRequest;
import com.sequenceiq.cloudbreak.cloud.event.platform.GetPlatformOrchestratorsResult;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.Orchestrator;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.PlatformOrchestrator;
import com.sequenceiq.cloudbreak.cloud.model.PlatformOrchestrators;
import com.sequenceiq.cloudbreak.cloud.model.Variant;

import reactor.bus.Event;

@Component
public class GetPlatformOrchestratorsHandler implements CloudPlatformEventHandler<GetPlatformOrchestratorsRequest> {
    private static final Logger LOGGER = LoggerFactory.getLogger(GetPlatformOrchestratorsHandler.class);

    @Inject
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Override
    public Class<GetPlatformOrchestratorsRequest> type() {
        return GetPlatformOrchestratorsRequest.class;
    }

    @Override
    public void accept(Event<GetPlatformOrchestratorsRequest> getPlatformOrchestratorsRequest) {
        LOGGER.info("Received event: {}", getPlatformOrchestratorsRequest);
        GetPlatformOrchestratorsRequest request = getPlatformOrchestratorsRequest.getData();
        try {
            Map<Platform, Collection<Orchestrator>> platformCollectionHashMap = Maps.newHashMap();
            Map<Platform, Orchestrator> defaults = Maps.newHashMap();

            for (Map.Entry<Platform, Collection<Variant>> connector : cloudPlatformConnectors.getPlatformVariants().getPlatformToVariants().entrySet()) {
                PlatformOrchestrator platformOrchestrator = cloudPlatformConnectors.getDefault(connector.getKey()).parameters().orchestratorParams();

                platformCollectionHashMap.put(connector.getKey(), platformOrchestrator.types());
                defaults.put(connector.getKey(), platformOrchestrator.defaultType());
            }
            GetPlatformOrchestratorsResult getPlatformOrchestratorsResult = new GetPlatformOrchestratorsResult(request,
                    new PlatformOrchestrators(platformCollectionHashMap, defaults));
            request.getResult().onNext(getPlatformOrchestratorsResult);
            LOGGER.info("Query platform orchestrators types finished.");
        } catch (Exception e) {
            request.getResult().onNext(new GetPlatformOrchestratorsResult(e.getMessage(), e, request));
        }
    }
}
