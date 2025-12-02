package com.sequenceiq.freeipa.flow.freeipa.rebuild.handler;

import static com.sequenceiq.freeipa.flow.freeipa.common.FailureType.ERROR;
import static com.sequenceiq.freeipa.flow.freeipa.common.FailureType.VALIDATION;

import java.util.Map;
import java.util.Set;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.host.OrchestratorStateParams;
import com.sequenceiq.cloudbreak.orchestrator.host.OrchestratorStateRetryParams;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.freeipa.rebuild.event.backup.ValidateBackupFailed;
import com.sequenceiq.freeipa.flow.freeipa.rebuild.event.backup.ValidateBackupRequest;
import com.sequenceiq.freeipa.flow.freeipa.rebuild.event.backup.ValidateBackupSuccess;
import com.sequenceiq.freeipa.orchestrator.StackBasedExitCriteriaModel;
import com.sequenceiq.freeipa.service.GatewayConfigService;
import com.sequenceiq.freeipa.service.stack.StackService;

@Component
public class ValidateBackupHandler extends ExceptionCatcherEventHandler<ValidateBackupRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ValidateBackupHandler.class);

    private static final String DL_AND_VALIDATE_BACKUP_STATE = "freeipa/rebuild/dl_and_validate_backup";

    private static final String FULL_BACKUP_LOCATION_PILLAR_KEY = "full_backup_location";

    private static final String DATA_BACKUP_LOCATION_PILLAR_KEY = "data_backup_location";

    @Value("${cb.max.salt.restore.dl_and_validate.retry}")
    private int maxRetryCount;

    @Value("${cb.max.salt.restore.dl_and_validate.retry.onerror}")
    private int maxRetryCountOnError;

    @Inject
    private HostOrchestrator hostOrchestrator;

    @Inject
    private GatewayConfigService gatewayConfigService;

    @Inject
    private StackService stackService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(ValidateBackupRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<ValidateBackupRequest> event) {
        return new ValidateBackupFailed(resourceId, e, ERROR);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<ValidateBackupRequest> event) {
        ValidateBackupRequest request = event.getData();
        try {
            Stack stack = stackService.getByIdWithListsInTransaction(request.getResourceId());
            GatewayConfig primaryGatewayConfig = gatewayConfigService.getPrimaryGatewayConfig(stack);
            OrchestratorStateParams stateParams = createOrchestratorStateParams(primaryGatewayConfig, request, stack.getId());
            hostOrchestrator.runOrchestratorState(stateParams);
            return new ValidateBackupSuccess(event.getData().getResourceId());
        } catch (CloudbreakOrchestratorFailedException e) {
            LOGGER.error("Failed to validate backup for {}", request, e);
            return new ValidateBackupFailed(request.getResourceId(), e, VALIDATION);
        }
    }

    private OrchestratorStateParams createOrchestratorStateParams(GatewayConfig primaryGatewayConfig, ValidateBackupRequest request, Long stackId) {
        OrchestratorStateParams stateParams = new OrchestratorStateParams();
        stateParams.setPrimaryGatewayConfig(primaryGatewayConfig);
        stateParams.setTargetHostNames(Set.of(primaryGatewayConfig.getHostname()));
        stateParams.setState(DL_AND_VALIDATE_BACKUP_STATE);
        stateParams.setStateParams(Map.of("freeipa", Map.of("rebuild", Map.of(
                FULL_BACKUP_LOCATION_PILLAR_KEY, request.getFullBackupStorageLocation(),
                DATA_BACKUP_LOCATION_PILLAR_KEY, request.getDataBackupStorageLocation()
        ))));
        stateParams.setExitCriteriaModel(new StackBasedExitCriteriaModel(stackId));
        OrchestratorStateRetryParams stateRetryParams = new OrchestratorStateRetryParams();
        stateRetryParams.setMaxRetry(maxRetryCount);
        stateRetryParams.setMaxRetryOnError(maxRetryCountOnError);
        stateParams.setStateRetryParams(stateRetryParams);
        LOGGER.debug("Created state params for backup download and validation: {}", stateParams);
        return stateParams;
    }
}
