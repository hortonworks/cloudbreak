package com.sequenceiq.cloudbreak.service.notification;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.common.notification.NotificationState;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.conf.EnvironmentInternalClientConfiguration;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.StackStatus;
import com.sequenceiq.notification.generator.dto.NotificationGeneratorDtos;
import com.sequenceiq.notification.service.NotificationSendingService;

@Service
public class StackNotificationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackNotificationService.class);

    @Value("${notification.register.stackstatus.enabled:true}")
    private boolean registerEnabled;

    private final EntitlementService entitlementService;

    private final NotificationSendingService notificationSendingService;

    private final StackNotificationDataPreparationService stackNotificationDataPreparetionService;

    private final StackNotificationTypePreparationService stackNotificationTypePreparationService;

    private final AsyncTaskExecutor intermediateBuilderExecutor;

    private final EnvironmentInternalClientConfiguration environmentInternalClientConfiguration;

    public StackNotificationService(
            EntitlementService entitlementService,
            NotificationSendingService notificationSendingService,
            AsyncTaskExecutor intermediateBuilderExecutor,
            StackNotificationDataPreparationService stackNotificationDataPreparetionService,
            StackNotificationTypePreparationService stackNotificationTypePreparationService,
            EnvironmentInternalClientConfiguration environmentInternalClientConfiguration) {
        this.entitlementService = entitlementService;
        this.notificationSendingService = notificationSendingService;
        this.intermediateBuilderExecutor = intermediateBuilderExecutor;
        this.stackNotificationDataPreparetionService = stackNotificationDataPreparetionService;
        this.stackNotificationTypePreparationService = stackNotificationTypePreparationService;
        this.environmentInternalClientConfiguration = environmentInternalClientConfiguration;
    }

    public void notify(Stack stack) {
        StackStatus stackStatus = stack.getStackStatus();
        notify(stack, stackStatus.getStatus(), stackStatus.getDetailedStackStatus(), stackStatus.getStatusReason());
    }

    public void notify(Stack stack, Status status, DetailedStackStatus detailedStackStatus, String statusReason) {
        String accountId = Crn.fromString(stack.getResourceCrn()).getAccountId();
        if (shouldWeSendNotification(status, stack, accountId)) {
            try {
                Boolean success = sendNotification(
                        stack,
                        status,
                        detailedStackStatus,
                        statusReason,
                        accountId).get();
                if (success) {
                    LOGGER.info("Notification was sent about Cluster health change for cluster: {}", stack.getName());
                } else {
                    LOGGER.info("Notification was not sent about Cluster health change for cluster: {}", stack.getName());
                }
            } catch (TransactionService.TransactionExecutionException | ExecutionException | InterruptedException e) {
                LOGGER.error("Notification could not be sent for cluster {} because {}.", stack.getName(), e.getMessage(), e);
            }
        }
    }

    private Future<Boolean> sendNotification(Stack stack, Status newStatus,
        DetailedStackStatus newDetailedStatus, String statusReason, String accountId) throws TransactionService.TransactionExecutionException {
        return intermediateBuilderExecutor.submit(() -> {
            try {
                environmentInternalClientConfiguration.cloudbreakInternalCrnClientClient()
                        .withInternalCrn()
                        .environmentInternalEndpoint()
                        .createOrUpdateDistributionListByEnvironmentCrn(stack.getEnvironmentCrn());

                NotificationGeneratorDtos notificationGeneratorDtos = stackNotificationDataPreparetionService
                        .notificationGeneratorDtos(stack, newStatus, newDetailedStatus, statusReason, accountId);

                notificationSendingService.processAndImmediatelySend(notificationGeneratorDtos);
                return true;
            } catch (Exception e) {
                LOGGER.error("Notification could not be sent for cluster {} because {}.", stack.getName(), e.getMessage(), e);
                throw e;
            }
        });
    }

    private boolean shouldWeSendNotification(Status newStatus, Stack stack, String accountId) {
        return stackNotificationTypePreparationService.isNotificationRequiredByStackStatus(newStatus)
                && entitlementService.isCdpCbNotificationSendingEnabled(accountId)
                && NotificationState.ENABLED.equals(stack.getNotificationState());
    }

}
