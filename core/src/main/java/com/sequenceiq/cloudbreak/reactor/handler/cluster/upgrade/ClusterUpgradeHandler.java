package com.sequenceiq.cloudbreak.reactor.handler.cluster.upgrade;

import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_UPGRADE;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_UPGRADE_NOT_NEEDED;
import static java.lang.String.format;

import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.cluster.api.ClusterApi;
import com.sequenceiq.cloudbreak.cluster.model.ParcelOperationStatus;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.cluster.ClusterBuilderService;
import com.sequenceiq.cloudbreak.domain.view.ClusterComponentView;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.message.FlowMessageService;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ClusterUpgradeFailedEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ClusterUpgradeRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ClusterUpgradeSuccess;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.parcel.ParcelService;
import com.sequenceiq.cloudbreak.service.parcel.UpgradeCandidateProvider;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class ClusterUpgradeHandler extends ExceptionCatcherEventHandler<ClusterUpgradeRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterUpgradeHandler.class);

    @Inject
    private StackDtoService stackDtoService;

    @Inject
    private ClusterApiConnectors clusterApiConnectors;

    @Inject
    private ParcelService parcelService;

    @Inject
    private ClusterBuilderService clusterBuilderService;

    @Inject
    private UpgradeCandidateProvider upgradeCandidateProvider;

    @Inject
    private ClusterService clusterService;

    @Inject
    private FlowMessageService flowMessageService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(ClusterUpgradeRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<ClusterUpgradeRequest> event) {
        return new ClusterUpgradeFailedEvent(resourceId, e, DetailedStackStatus.CLUSTER_UPGRADE_FAILED);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<ClusterUpgradeRequest> event) {
        LOGGER.debug("Accepting Cluster upgrade event {}", event);
        ClusterUpgradeRequest request = event.getData();
        Long stackId = request.getResourceId();
        try {
            StackDto stackDto = stackDtoService.getById(stackId);
            Set<ClusterComponentView> components = parcelService.getParcelComponentsByBlueprint(stackDto);
            ClusterApi connector = clusterApiConnectors.getConnector(stackDto);
            Set<ClouderaManagerProduct> upgradeCandidateProducts = upgradeCandidateProvider.getRequiredProductsForUpgrade(connector, stackDto, components);
            if (upgradeCandidateProducts.isEmpty()) {
                LOGGER.debug("Skip cluster runtime upgrade because all required product is present on the cluster.");
                flowMessageService.fireEventAndLog(stackId, Status.UPDATE_IN_PROGRESS.name(), CLUSTER_UPGRADE_NOT_NEEDED);
                return new ClusterUpgradeSuccess(stackId);
            } else {
                clusterService.updateClusterStatusByStackId(stackId, DetailedStackStatus.CLUSTER_UPGRADE_IN_PROGRESS);
                flowMessageService.fireEventAndLog(stackId, Status.UPDATE_IN_PROGRESS.name(), CLUSTER_UPGRADE);
                Optional<String> remoteDataContext = getRemoteDataContext(stackDto.getStack());
                connector.upgradeClusterRuntime(upgradeCandidateProducts, request.isPatchUpgrade(), remoteDataContext, request.isRollingUpgradeEnabled());
                return removeUnusedParcelsAfterRuntimeUpgrade(stackDto, components);
            }
        } catch (Exception e) {
            LOGGER.error("Cluster upgrade event failed", e);
            return new ClusterUpgradeFailedEvent(request.getResourceId(), e, DetailedStackStatus.CLUSTER_UPGRADE_FAILED);
        }
    }

    private Optional<String> getRemoteDataContext(StackView stack) {
        Optional<String> remoteDataContext = Optional.empty();
        if (!stack.isDatalake() && StringUtils.isNotEmpty(stack.getDatalakeCrn())) {
            LOGGER.info("Fetch the Remote Data Context from {} to update the Data Hub", stack.getName());
            remoteDataContext = clusterBuilderService.getSdxContextOptional(stack.getDatalakeCrn());
        }
        return remoteDataContext;
    }

    private Selectable removeUnusedParcelsAfterRuntimeUpgrade(StackDto stackDto, Set<ClusterComponentView> components) throws CloudbreakException {
        ParcelOperationStatus parcelOperationStatus = parcelService.removeUnusedParcelComponents(stackDto, components);
        if (parcelOperationStatus.getFailed().isEmpty()) {
            return new ClusterUpgradeSuccess(stackDto.getId());
        } else {
            LOGGER.info("There are failed parcel removals: {}", parcelOperationStatus);
            CloudbreakException exception = new CloudbreakException(format("Failed to remove the following parcels: %s", parcelOperationStatus.getFailed()));
            return new ClusterUpgradeFailedEvent(stackDto.getId(), exception, DetailedStackStatus.CLUSTER_UPGRADE_FAILED);
        }
    }
}
