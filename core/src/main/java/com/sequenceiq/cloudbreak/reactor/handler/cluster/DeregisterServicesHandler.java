package com.sequenceiq.cloudbreak.reactor.handler.cluster;

import java.util.Optional;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.client.HttpClientConfig;
import com.sequenceiq.cloudbreak.cluster.api.ClusterApi;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.dto.datalake.DatalakeDto;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.DeregisterServicesRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.DeregisterServicesResult;
import com.sequenceiq.cloudbreak.service.TlsSecurityService;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.sharedservice.DatalakeService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.EventHandler;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class DeregisterServicesHandler implements EventHandler<DeregisterServicesRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeregisterServicesHandler.class);

    @Inject
    private EventBus eventBus;

    @Inject
    private ClusterApiConnectors clusterApiConnectors;

    @Inject
    private StackService stackService;

    @Inject
    private TlsSecurityService tlsSecurityService;

    @Inject
    private DatalakeService datalakeService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(DeregisterServicesRequest.class);
    }

    @Override
    public void accept(Event<DeregisterServicesRequest> event) {
        DeregisterServicesResult result;
        try {
            LOGGER.info("Received DeregisterServicesRequest event: {}", event.getData());
            Stack stack = stackService.getByIdWithListsInTransaction(event.getData().getResourceId());
            ClusterApi clusterApi = clusterApiConnectors.getConnector(stack);
            Optional<DatalakeDto> datalakeDto = Optional.empty();
            Optional<Stack> dataLakeOptional = datalakeService.getDatalakeStackByDatahubStack(stack);
            if (dataLakeOptional.isPresent()) {
                Stack dataLake = dataLakeOptional.get();
                HttpClientConfig httpClientConfig = tlsSecurityService.buildTLSClientConfigForPrimaryGateway(dataLake.getId(),
                        dataLake.getClusterManagerIp(), dataLake.cloudPlatform());
                datalakeDto = Optional.ofNullable(
                        DatalakeDto.DatalakeDtoBuilder.aDatalakeDto()
                                .withGatewayPort(dataLake.getGatewayPort())
                                .withHttpClientConfig(httpClientConfig)
                                .withPassword(dataLake.getCluster().getPassword())
                                .withUser(dataLake.getCluster().getUserName())
                                .withName(dataLake.getName())
                                .build()
                );
            }
            clusterApi.clusterSecurityService().deregisterServices(stack.getName(), datalakeDto);
            LOGGER.info("Finished disabling Security");
            result = new DeregisterServicesResult(event.getData());
        } catch (Exception e) {
            LOGGER.warn("An error has occured during disabling security", e);
            result = new DeregisterServicesResult(e.getMessage(), e, event.getData());
        }
        LOGGER.info("Sending out DeregisterServicesResult: {}", result);
        eventBus.notify(result.selector(), new Event<>(event.getHeaders(), result));
        LOGGER.info("DeregisterServicesResult has been sent");
    }
}
