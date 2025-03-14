package com.sequenceiq.cloudbreak.core.flow2.cluster.skumigration.handler.detachpublicips;

import static com.sequenceiq.cloudbreak.util.Benchmark.measure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.cluster.skumigration.SkuMigrationFailedEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.skumigration.SkuMigrationFlowEvent;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class DetachPublicIpsHandler extends ExceptionCatcherEventHandler<DetachPublicIpsRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DetachPublicIpsHandler.class);

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<DetachPublicIpsRequest> event) {
        return new SkuMigrationFailedEvent(SkuMigrationFlowEvent.SKU_MIGRATION_FAILED_EVENT.event(), resourceId, e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<DetachPublicIpsRequest> event) {
        DetachPublicIpsRequest request = event.getData();
        try {
            CloudConnector connector = request.getCloudConnector();
            AuthenticatedContext ac = connector.authentication().authenticate(request.getCloudContext(), request.getCloudCredential());
            CloudStack cloudStack = request.getCloudStack();
            LOGGER.info("Detaching public IP addresses");
            measure(() -> connector.resources().detachPublicIpAddressesForVMsIfNotPrivate(ac, cloudStack), LOGGER,
                    "Detaching public IP addresses took {} ms");
        } catch (Exception e) {
            return new SkuMigrationFailedEvent(SkuMigrationFlowEvent.SKU_MIGRATION_FAILED_EVENT.event(),
                    request.getResourceId(), e);
        }

        return new DetachPublicIpsResult(request.getResourceId());
    }

    @Override
    public String selector() {
        return EventSelectorUtil.selector(DetachPublicIpsRequest.class);
    }
}
