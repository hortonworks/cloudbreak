package com.sequenceiq.cloudbreak.reactor.handler.cluster.upgrade;

import static java.lang.String.format;

import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.cluster.api.ClusterApi;
import com.sequenceiq.cloudbreak.cluster.model.ParcelOperationStatus;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.cluster.ClusterBuilderService;
import com.sequenceiq.cloudbreak.domain.view.ClusterComponentView;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ClusterUpgradeFailedEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ClusterUpgradeRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ClusterUpgradeSuccess;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.parcel.ParcelService;
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
        LOGGER.debug("Accepting Cluster upgrade event..");
        ClusterUpgradeRequest request = event.getData();
        Long stackId = request.getResourceId();
        Selectable result;
        try {
            StackDto stackDto = stackDtoService.getById(stackId);
            StackView stack = stackDto.getStack();
            Optional<String> remoteDataContext = getRemoteDataContext(stack);
            ClusterApi connector = clusterApiConnectors.getConnector(stackDto);
            Set<ClusterComponentView> components = parcelService.getParcelComponentsByBlueprint(stackDto);
            connector.upgradeClusterRuntime(components, request.isPatchUpgrade(), remoteDataContext, request.isRollingUpgradeEnabled());
            ParcelOperationStatus parcelOperationStatus = parcelService.removeUnusedParcelComponents(stackDto, components);
            if (parcelOperationStatus.getFailed().isEmpty()) {
                result = new ClusterUpgradeSuccess(request.getResourceId());
            } else {
                LOGGER.info("There are failed parcel removals: {}", parcelOperationStatus);
                CloudbreakException exception = new CloudbreakException(format("Failed to remove the following parcels: %s", parcelOperationStatus.getFailed()));
                result = new ClusterUpgradeFailedEvent(request.getResourceId(), exception, DetailedStackStatus.CLUSTER_UPGRADE_FAILED);
            }
        } catch (Exception e) {
            LOGGER.error("Cluster upgrade event failed", e);
            result = new ClusterUpgradeFailedEvent(request.getResourceId(), e, DetailedStackStatus.CLUSTER_UPGRADE_FAILED);
        }
        return result;
    }

    private Optional<String> getRemoteDataContext(StackView stack) {
        Optional<String> remoteDataContext = Optional.empty();
        if (!stack.isDatalake() && StringUtils.isNotEmpty(stack.getDatalakeCrn())) {
            LOGGER.info("Fetch the Remote Data Context from {} to update the Data Hub", stack.getName());
            remoteDataContext = clusterBuilderService.getSdxContextOptional(stack.getDatalakeCrn());
        }
        return remoteDataContext;
    }
}
