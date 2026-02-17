package com.sequenceiq.cloudbreak.reactor.handler.cluster.upgrade;

import java.util.Set;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.cluster.api.ClusterApi;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.domain.view.ClusterComponentView;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ClusterUpgradeFailedEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ClusterUpgradeInitRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ClusterUpgradeInitSuccess;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.parcel.ParcelService;
import com.sequenceiq.cloudbreak.service.parcel.UpgradeCandidateProvider;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.service.upgrade.ClusterUpgradePrerequisitesService;
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

    @Inject
    private ClusterUpgradePrerequisitesService clusterUpgradePrerequisitesService;

    @Inject
    private ClusterApiConnectors clusterApiConnectors;

    @Inject
    private UpgradeCandidateProvider upgradeCandidateProvider;

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
            Set<ClusterComponentView> components = parcelService.getParcelComponentsByBlueprint(stackDto);
            parcelService.removeUnusedParcelComponents(stackDto, components);
            ClusterApi connector = clusterApiConnectors.getConnector(stackDto);
            clusterUpgradePrerequisitesService.removeIncompatibleServices(stackDto, connector, request.getTargetRuntimeVersion());
            Set<ClouderaManagerProduct> upgradeCandidateProducts = upgradeCandidateProvider.getRequiredProductsForUpgrade(connector, stackDto, components);
            result = new ClusterUpgradeInitSuccess(request.getResourceId(), upgradeCandidateProducts, request.getOriginalOsType());
        } catch (Exception e) {
            LOGGER.error("Upgrade initialization failed", e);
            result = new ClusterUpgradeFailedEvent(request.getResourceId(), e, DetailedStackStatus.CLUSTER_UPGRADE_INIT_FAILED);
        }
        return result;
    }
}
