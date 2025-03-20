package com.sequenceiq.cloudbreak.core.flow2.cluster.skumigration.handler.removeloadbalancer;

import static com.sequenceiq.cloudbreak.util.Benchmark.measure;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.cluster.skumigration.SkuMigrationFailedEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.skumigration.SkuMigrationFlowEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.skumigration.SkuMigrationService;
import com.sequenceiq.cloudbreak.domain.stack.loadbalancer.LoadBalancer;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.service.stack.LoadBalancerPersistenceService;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class RemoveLoadBalancerHandler extends ExceptionCatcherEventHandler<RemoveLoadBalancerRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RemoveLoadBalancerHandler.class);

    @Inject
    private LoadBalancerPersistenceService loadBalancerPersistenceService;

    @Inject
    private SkuMigrationService skuMigrationService;

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<RemoveLoadBalancerRequest> event) {
        return new SkuMigrationFailedEvent(SkuMigrationFlowEvent.SKU_MIGRATION_FAILED_EVENT.event(), resourceId, e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<RemoveLoadBalancerRequest> event) {
        RemoveLoadBalancerRequest request = event.getData();
        try {
            Set<LoadBalancer> loadBalancers = loadBalancerPersistenceService.findByStackId(request.getResourceId());
            CloudConnector connector = request.getCloudConnector();
            AuthenticatedContext ac = connector.authentication().authenticate(request.getCloudContext(), request.getCloudCredential());
            List<String> loadBalancerNames = loadBalancers.stream()
                    .filter(loadBalancer -> loadBalancer.getProviderConfig() != null && loadBalancer.getProviderConfig().getAzureConfig() != null)
                    .map(loadBalancer -> loadBalancer.getProviderConfig().getAzureConfig().getName())
                    .collect(Collectors.toList());
            LOGGER.info("Removing load balancers: {}", loadBalancerNames);
            measure(() -> connector.resources().deleteLoadBalancers(ac, request.getCloudStack(), loadBalancerNames), LOGGER,
                    "Deleting load balancers took {} ms");
            skuMigrationService.updateSkuToStandard(request.getResourceId(), loadBalancers);
        } catch (Exception e) {
            return new SkuMigrationFailedEvent(SkuMigrationFlowEvent.SKU_MIGRATION_FAILED_EVENT.event(),
                    request.getResourceId(), e);
        }

        return new RemoveLoadBalancerResult(request.getResourceId());
    }

    @Override
    public String selector() {
        return EventSelectorUtil.selector(RemoveLoadBalancerRequest.class);
    }
}
