package com.sequenceiq.cloudbreak.maintenancewindow;

import static com.sequenceiq.cloudbreak.rotation.maintenance.MaintenanceWindowSecretRotationSupport.TASK_TYPE;
import static com.sequenceiq.cloudbreak.rotation.maintenance.MaintenanceWindowSecretRotationSupport.taskPayload;
import static com.sequenceiq.cloudbreak.rotation.maintenance.MaintenanceWindowSecretRotationSupport.workItemIdForSecretName;
import static com.sequenceiq.maintenance.api.model.MaintenanceSubmitterService.CLOUDBREAK;

import java.util.List;

import jakarta.inject.Inject;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.maintenance.api.internal.schedule.endpoint.MaintenanceWindowScheduleInternalEndpoint;
import com.sequenceiq.maintenance.api.v1.task.endpoint.MaintenanceWindowTaskEndpoint;
import com.sequenceiq.maintenance.client.schedule.MaintenanceWindowScheduleLookup;
import com.sequenceiq.maintenance.client.task.MaintenanceWindowTaskRegistration;
import com.sequenceiq.maintenance.client.task.MaintenanceWindowTaskRegistration.WorkItem;

@Service
@ConditionalOnExpression(MaintenanceServiceConditions.URL_CONFIGURED)
public class MaintenanceWindowSecretRotationTaskRegistrar {

    private final MaintenanceWindowTaskRegistration registration;

    private final StackDtoService stackDtoService;

    @Inject
    public MaintenanceWindowSecretRotationTaskRegistrar(
            MaintenanceWindowTaskEndpoint maintenanceWindowTaskEndpoint,
            MaintenanceWindowScheduleInternalEndpoint scheduleInternalEndpoint,
            StackDtoService stackDtoService,
            @Value("${secret-rotation.periodic.maintenance-window.max-attempts-per-occurrence:3}") int maxAttemptsPerOccurrence,
            @Value("${secret-rotation.periodic.maintenance-window.retry-cooldown-minutes:15}") int retryCooldownMinutes) {
        this.stackDtoService = stackDtoService;
        this.registration = new MaintenanceWindowTaskRegistration(
                maintenanceWindowTaskEndpoint,
                new MaintenanceWindowScheduleLookup(scheduleInternalEndpoint, CLOUDBREAK),
                CLOUDBREAK,
                maxAttemptsPerOccurrence,
                retryCooldownMinutes);
    }

    public void registerDueRotation(String resourceCrn, List<String> dueSecretNames) {
        StackDto stack = stackDtoService.getByCrn(resourceCrn);
        String accountId = Crn.fromString(resourceCrn).getAccountId();
        List<WorkItem> workItems = dueSecretNames.stream()
                .map(secretName -> new WorkItem(workItemIdForSecretName(secretName), taskPayload(secretName)))
                .toList();
        registration.register(accountId, resourceCrn, stack.getEnvironmentCrn(), TASK_TYPE, workItems);
    }
}
