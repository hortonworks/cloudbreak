package com.sequenceiq.cloudbreak.core.flow2.cluster.skumigration.handler.refreshdns;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.cluster.skumigration.SkuMigrationFailedEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.skumigration.SkuMigrationFlowEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.skumigration.handler.SkuMigrationFinished;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.service.publicendpoint.ClusterPublicEndpointManagementService;
import com.sequenceiq.cloudbreak.service.publicendpoint.GatewayPublicEndpointManagementService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class UpdateDnsHandler extends ExceptionCatcherEventHandler<UpdateDnsRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateDnsHandler.class);

    @Inject
    private GatewayPublicEndpointManagementService gatewayPublicEndpointManagementService;

    @Inject
    private ClusterPublicEndpointManagementService clusterPublicEndpointManagementService;

    @Inject
    private StackDtoService stackDtoService;

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<UpdateDnsRequest> event) {
        return new SkuMigrationFailedEvent(SkuMigrationFlowEvent.SKU_MIGRATION_FAILED_EVENT.event(), resourceId, e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<UpdateDnsRequest> event) {
        UpdateDnsRequest request = event.getData();
        try {
            StackView stack = request.getStack();
            LOGGER.info("Refreshing Load balancer DNS in FreeIPA");
            clusterPublicEndpointManagementService.registerLoadBalancerWithFreeIPA(stack);
            LOGGER.info("Refreshing Load balancer DNS in PEM");
            gatewayPublicEndpointManagementService.updateDnsEntryForLoadBalancers(stackDtoService.getById(stack.getId()));
        } catch (Exception e) {
            return new SkuMigrationFailedEvent(SkuMigrationFlowEvent.SKU_MIGRATION_FAILED_EVENT.event(),
                    request.getResourceId(), e);
        }
        return new SkuMigrationFinished(request.getResourceId());
    }

    @Override
    public String selector() {
        return EventSelectorUtil.selector(UpdateDnsRequest.class);
    }
}
