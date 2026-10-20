package com.sequenceiq.freeipa.maintenancewindow.handler;

import static com.sequenceiq.cloudbreak.rotation.maintenance.MaintenanceWindowSecretRotationSupport.TASK_TYPE;

import java.util.List;
import java.util.Map;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.rotation.maintenance.MaintenanceWindowSecretRotationSupport;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.rotate.FreeIpaSecretRotationRequest;
import com.sequenceiq.freeipa.api.v1.maintenance.model.MaintenanceTaskDispatchRequest;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaService;
import com.sequenceiq.freeipa.service.rotation.FreeIpaSecretRotationService;
import com.sequenceiq.maintenance.api.execution.MaintenanceWindowTaskExecuteHandler;

@Service
public class SecretRotationMaintenanceWindowTaskExecuteHandler implements MaintenanceWindowTaskExecuteHandler<MaintenanceTaskDispatchRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SecretRotationMaintenanceWindowTaskExecuteHandler.class);

    @Inject
    private FreeIpaService freeIpaService;

    @Inject
    private FreeIpaSecretRotationService freeIpaSecretRotationService;

    @Override
    public String taskType() {
        return TASK_TYPE;
    }

    @Override
    public FlowIdentifier execute(MaintenanceTaskDispatchRequest request) {
        String secretName = MaintenanceWindowSecretRotationSupport.resolveSecretName(
                request.getWorkItemId(), request.getTaskPayload());
        Map<String, String> additionalProperties = MaintenanceWindowSecretRotationSupport.maintenanceWindowAdditionalProperties(
                request.getAccountId(), request.getTaskId(), request.getRunId());
        // rotateSecretsByCrn is env-scoped and reloads the stack; one FreeIPA per environment is assumed.
        String environmentCrn = freeIpaService.getViewByResourceCrn(request.getResourceCrn())
                .orElseThrow(NotFoundException.notFound("FreeIPA", request.getResourceCrn()))
                .environmentCrn();
        FreeIpaSecretRotationRequest rotationRequest = new FreeIpaSecretRotationRequest();
        rotationRequest.setSecrets(List.of(secretName));
        rotationRequest.setAdditionalProperties(additionalProperties);
        FlowIdentifier flowIdentifier = freeIpaSecretRotationService.rotateSecretsByCrn(
                request.getAccountId(), environmentCrn, rotationRequest);
        LOGGER.info("Accepted maintenance window secret rotation taskId={} runId={} resourceCrn={} secret={} flow={}",
                request.getTaskId(), request.getRunId(), request.getResourceCrn(), secretName, flowIdentifier);
        return flowIdentifier;
    }
}
