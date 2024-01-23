package com.sequenceiq.cloudbreak.reactor.handler.cluster.upgrade;

import java.util.Set;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.domain.view.ClusterComponentView;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ClusterUpgradeFailedEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ClusterUpgradeInitRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ClusterUpgradeInitSuccess;
import com.sequenceiq.cloudbreak.service.parcel.ParcelService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class ClusterUpgradeInitHandler extends ExceptionCatcherEventHandler<ClusterUpgradeInitRequest> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterUpgradeInitHandler.class);

    @Inject
    private StackDtoService stackDtoService;

    @Inject
    private ParcelService parcelService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(ClusterUpgradeInitRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<ClusterUpgradeInitRequest> event) {
        return new ClusterUpgradeFailedEvent(resourceId, e, DetailedStackStatus.CLUSTER_UPGRADE_INIT_FAILED);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<ClusterUpgradeInitRequest> event) {
        LOGGER.debug("Accepting Cluster Manager parcel deactivation event..");
        StackDto stackDto = stackDtoService.getById(event.getData().getResourceId());
        ClusterUpgradeInitRequest request = event.getData();
        Selectable result;
        try {
            Set<ClusterComponentView> componentsByBlueprint = parcelService.getParcelComponentsByBlueprint(stackDto);
            parcelService.removeUnusedParcelComponents(stackDto, componentsByBlueprint);
            result = new ClusterUpgradeInitSuccess(request.getResourceId());
        } catch (Exception e) {
            LOGGER.error("Cluster Manager parcel deactivation failed", e);
            result = new ClusterUpgradeFailedEvent(request.getResourceId(), e, DetailedStackStatus.CLUSTER_UPGRADE_INIT_FAILED);
        }
        return result;
    }
}
