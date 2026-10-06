package com.sequenceiq.datalake.maintenancewindow.handler;

import static com.sequenceiq.cloudbreak.rotation.maintenance.MaintenanceWindowSecretRotationSupport.TASK_TYPE;

import java.util.List;
import java.util.Map;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.rotation.maintenance.MaintenanceWindowSecretRotationSupport;
import com.sequenceiq.datalake.service.rotation.SdxRotationService;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.maintenance.api.execution.MaintenanceWindowTaskExecuteHandler;
import com.sequenceiq.sdx.api.v1.maintenance.model.MaintenanceTaskDispatchRequest;

@Service
public class SecretRotationMaintenanceWindowTaskExecuteHandler implements MaintenanceWindowTaskExecuteHandler<MaintenanceTaskDispatchRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SecretRotationMaintenanceWindowTaskExecuteHandler.class);

    @Inject
    private SdxRotationService sdxRotationService;

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
        FlowIdentifier flowIdentifier = sdxRotationService.triggerSecretRotation(
                request.getResourceCrn(), List.of(secretName), null, additionalProperties);
        LOGGER.info("Accepted maintenance window secret rotation taskId={} runId={} resourceCrn={} secret={} flow={}",
                request.getTaskId(), request.getRunId(), request.getResourceCrn(), secretName, flowIdentifier);
        return flowIdentifier;
    }
}
