package com.sequenceiq.cloudbreak.reactor.handler.cluster.install;

import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.bootstrap.service.host.ClusterHostServiceRunner;
import com.sequenceiq.cloudbreak.core.cluster.ClusterBuilderService;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.install.FinalizeClusterInstallFailed;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.install.FinalizeClusterInstallRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.install.FinalizeClusterInstallSuccess;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.service.parcel.ParcelService;
import com.sequenceiq.cloudbreak.service.recovery.RdsRecoverySetupService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class FinalizeClusterInstallHandler extends ExceptionCatcherEventHandler<FinalizeClusterInstallRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(FinalizeClusterInstallHandler.class);

    @Inject
    private ClusterBuilderService clusterBuilderService;

    @Inject
    private RdsRecoverySetupService rdsRecoverySetupService;

    @Inject
    private ParcelService parcelService;

    @Inject
    private StackDtoService stackDtoService;

    @Inject
    private ClusterHostServiceRunner clusterHostServiceRunner;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(FinalizeClusterInstallRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<FinalizeClusterInstallRequest> event) {
        LOGGER.error("ClusterInstallSuccessHandler step failed with the following message: {}", e.getMessage());
        return new FinalizeClusterInstallFailed(resourceId, e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<FinalizeClusterInstallRequest> event) {
        Long stackId = event.getData().getResourceId();
        Selectable response;
        try {
            StackDto stackDto = stackDtoService.getById(stackId);
            clusterHostServiceRunner.createCronForUserHomeCreation(stackDto, stackDto.getAllAvailableInstances().stream()
                    .map(InstanceMetadataView::getDiscoveryFQDN).collect(Collectors.toSet()));
            clusterBuilderService.finalizeClusterInstall(stackDto);
            if (event.getData().getProvisionType().isRecovery()) {
                rdsRecoverySetupService.removeRecoverRole(stackId);
            }
            LOGGER.debug("Removing unused parcels from CM after cluster install.");
            parcelService.removeUnusedParcelComponents(stackDto);
            response = new FinalizeClusterInstallSuccess(stackId);
        } catch (RuntimeException | CloudbreakException | CloudbreakOrchestratorFailedException e) {
            LOGGER.error("ClusterInstallSuccessHandler step failed with the following message: {}", e.getMessage());
            response = new FinalizeClusterInstallFailed(stackId, e);
        }
        return response;
    }
}
