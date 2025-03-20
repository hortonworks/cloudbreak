package com.sequenceiq.cloudbreak.core.flow2.cluster.skumigration.handler.check;

import static com.sequenceiq.cloudbreak.event.ResourceEvent.LOAD_BALANCER_SKU_IS_STANDARD;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudLoadBalancer;
import com.sequenceiq.cloudbreak.cloud.model.CloudLoadBalancerMetadata;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.cluster.skumigration.SkuMigrationFailedEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.skumigration.SkuMigrationFlowEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.skumigration.SkuMigrationService;
import com.sequenceiq.cloudbreak.core.flow2.cluster.skumigration.handler.SkuMigrationFinished;
import com.sequenceiq.cloudbreak.domain.stack.loadbalancer.LoadBalancer;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.message.FlowMessageService;
import com.sequenceiq.cloudbreak.service.stack.LoadBalancerPersistenceService;
import com.sequenceiq.common.api.type.LoadBalancerSku;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class CheckSkuHandler extends ExceptionCatcherEventHandler<CheckSkuRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CheckSkuHandler.class);

    @Inject
    private LoadBalancerPersistenceService loadBalancerPersistenceService;

    @Inject
    private FlowMessageService flowMessageService;

    @Inject
    private SkuMigrationService skuMigrationService;

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<CheckSkuRequest> event) {
        return new SkuMigrationFailedEvent(SkuMigrationFlowEvent.SKU_MIGRATION_FAILED_EVENT.event(), resourceId, e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<CheckSkuRequest> event) {
        CheckSkuRequest request = event.getData();
        if (request.isForce()) {
            return new CheckSkuResult(request.getResourceId());
        } else {
            Set<LoadBalancer> loadBalancers = loadBalancerPersistenceService.findByStackId(request.getResourceId());
            CloudConnector connector = request.getCloudConnector();
            List<CloudLoadBalancerMetadata> loadBalancerMetadataList = getCloudLoadBalancerMetadata(loadBalancers);
            CloudContext cloudContext = request.getCloudContext();
            AuthenticatedContext ac = connector.authentication().authenticate(cloudContext, request.getCloudCredential());
            List<CloudLoadBalancer> describedLoadBalancers = connector.resources().describeLoadBalancers(ac, request.getCloudStack(), loadBalancerMetadataList);
            boolean nonStandardLoadBalancerFound = describedLoadBalancers.stream()
                    .anyMatch(describedLoadBalancer -> !LoadBalancerSku.STANDARD.equals(describedLoadBalancer.getSku()));
            LOGGER.info("Non-standard found: {}. Load balancers for stack: {}", nonStandardLoadBalancerFound, describedLoadBalancers);
            if (loadBalancers.isEmpty() || nonStandardLoadBalancerFound) {
                LOGGER.info("Proceed with migration");
                return new CheckSkuResult(request.getResourceId());
            } else {
                skuMigrationService.updateSkuToStandard(request.getResourceId(), loadBalancers);
                flowMessageService.fireEventAndLog(request.getResourceId(), Status.UPDATE_IN_PROGRESS.name(), LOAD_BALANCER_SKU_IS_STANDARD);
                return new SkuMigrationFinished(request.getResourceId());
            }
        }
    }

    private List<CloudLoadBalancerMetadata> getCloudLoadBalancerMetadata(Set<LoadBalancer> loadBalancers) {
        List<CloudLoadBalancerMetadata> loadBalancerMetadataList = new ArrayList<>();
        for (LoadBalancer loadBalancer : loadBalancers) {
            if (loadBalancer.getProviderConfig() != null && loadBalancer.getProviderConfig().getAzureConfig() != null) {
                String loadBalancerName = loadBalancer.getProviderConfig().getAzureConfig().getName();
                CloudLoadBalancerMetadata loadBalancerMetadata = CloudLoadBalancerMetadata.builder()
                        .withType(loadBalancer.getType())
                        .withIp(loadBalancer.getIp())
                        .withName(loadBalancerName)
                        .build();
                loadBalancerMetadataList.add(loadBalancerMetadata);
            }
        }
        return loadBalancerMetadataList;
    }

    @Override
    public String selector() {
        return EventSelectorUtil.selector(CheckSkuRequest.class);
    }
}
