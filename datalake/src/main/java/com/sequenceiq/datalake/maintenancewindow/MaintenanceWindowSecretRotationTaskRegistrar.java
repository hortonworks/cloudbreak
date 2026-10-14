package com.sequenceiq.datalake.maintenancewindow;

import static com.sequenceiq.cloudbreak.rotation.maintenance.MaintenanceWindowSecretRotationSupport.TASK_TYPE;
import static com.sequenceiq.cloudbreak.rotation.maintenance.MaintenanceWindowSecretRotationSupport.taskPayload;
import static com.sequenceiq.cloudbreak.rotation.maintenance.MaintenanceWindowSecretRotationSupport.workItemIdForSecretName;
import static com.sequenceiq.maintenance.api.model.MaintenanceSubmitterService.DATALAKE;

import java.util.List;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;

import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.service.sdx.SdxService;
import com.sequenceiq.maintenance.api.internal.schedule.endpoint.MaintenanceWindowScheduleInternalEndpoint;
import com.sequenceiq.maintenance.api.v1.task.endpoint.MaintenanceWindowTaskEndpoint;
import com.sequenceiq.maintenance.client.schedule.MaintenanceWindowScheduleLookup;
import com.sequenceiq.maintenance.client.task.MaintenanceWindowTaskRegistration;
import com.sequenceiq.maintenance.client.task.MaintenanceWindowTaskRegistration.WorkItem;

@Service
@ConditionalOnExpression(MaintenanceServiceConditions.URL_CONFIGURED)
public class MaintenanceWindowSecretRotationTaskRegistrar {

    @Inject
    private MaintenanceWindowTaskEndpoint maintenanceWindowTaskEndpoint;

    @Inject
    private MaintenanceWindowScheduleInternalEndpoint scheduleInternalEndpoint;

    @Inject
    private SdxService sdxService;

    @Value("${secret-rotation.periodic.maintenance-window.max-attempts-per-occurrence:3}")
    private int maxAttemptsPerOccurrence;

    @Value("${secret-rotation.periodic.maintenance-window.retry-cooldown-minutes:15}")
    private int retryCooldownMinutes;

    private MaintenanceWindowTaskRegistration registration;

    @PostConstruct
    void initRegistration() {
        registration = new MaintenanceWindowTaskRegistration(
                maintenanceWindowTaskEndpoint,
                new MaintenanceWindowScheduleLookup(scheduleInternalEndpoint, DATALAKE),
                DATALAKE,
                maxAttemptsPerOccurrence,
                retryCooldownMinutes);
    }

    public void registerDueRotation(String resourceCrn, List<String> dueSecretNames) {
        SdxCluster cluster = sdxService.getByCrn(resourceCrn);
        String accountId = cluster.getAccountId();
        List<WorkItem> workItems = dueSecretNames.stream()
                .map(secretName -> new WorkItem(workItemIdForSecretName(secretName), taskPayload(secretName)))
                .toList();
        registration.register(accountId, resourceCrn, cluster.getEnvCrn(), TASK_TYPE, workItems);
    }
}
