package com.sequenceiq.redbeams.events.notification;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.common.notification.NotificationState;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredNotificationDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredNotificationEvent;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.notification.domain.DistributionListActionType;
import com.sequenceiq.notification.domain.NotificationGroupType;
import com.sequenceiq.notification.domain.NotificationSeverity;
import com.sequenceiq.notification.generator.dto.NotificationGeneratorDtos;
import com.sequenceiq.notification.sender.DistributionListManagementService;
import com.sequenceiq.notification.service.NotificationSendingService;
import com.sequenceiq.redbeams.domain.DatabaseServerConfig;
import com.sequenceiq.redbeams.domain.stack.DBStack;
import com.sequenceiq.redbeams.service.EnvironmentService;

@ExtendWith(MockitoExtension.class)
class DbStackEmailNotificationServiceTest {

    private static final String ACCOUNT_ID = "acc";

    private static final String OWNER_CRN = "crn:cdp:iam:us-west-1:acc:user:bob";

    private static final String DB_CRN = "crn:cdp:redbeams:us-west-1:acc:databaseServer:res";

    private static final String DB_NAME = "my-external-db";

    private static final String ENV_CRN = "crn:cdp:environments:us-west-1:acc:environment:env";

    private static final String ENV_NAME = "my-env";

    private static final String CLUSTER_CRN = "crn:cdp:datahub:us-west-1:acc:cluster:dh";

    private static final String CLUSTER_NAME = "my-cluster";

    @Mock
    private EntitlementService entitlementService;

    @Mock
    private DistributionListManagementService distributionListManagementService;

    @Mock
    private NotificationSendingService notificationSendingService;

    @Mock
    private DbStackEmailNotificationDataPreparationService dataPreparationService;

    @Mock
    private EnvironmentService environmentService;

    @Mock
    private DbStackNotificationTypePreparationService dbStackNotificationTypePreparationService;

    @Mock
    private DBStackOwnerIdentifierService dbStackOwnerIdentifierService;

    @Mock
    private AsyncTaskExecutor intermediateBuilderExecutor;

    @InjectMocks
    private DbStackEmailNotificationService underTest;

    private DBStack dbStack;

    private DatabaseServerConfig databaseServerConfig;

    private CDPStructuredNotificationEvent event;

    @BeforeEach
    void setUp() {
        dbStack = new DBStack();
        dbStack.setName(DB_NAME);
        dbStack.setResourceCrn(DB_CRN);
        dbStack.setOwnerCrn(Crn.safeFromString(OWNER_CRN));
        dbStack.setEnvironmentId(ENV_CRN);

        databaseServerConfig = new DatabaseServerConfig();
        databaseServerConfig.setClusterCrn(CLUSTER_CRN);

        CDPStructuredNotificationDetails details = new CDPStructuredNotificationDetails(
                ResourceEvent.REDBEAMS_EXTERNAL_DATABASE_STORAGE_LOW, DB_CRN, "redbeams", "payload");
        event = new CDPStructuredNotificationEvent(null, details, "status", "reason");
    }

    @SuppressWarnings("unchecked")
    private void setUpExecutor() {
        // run the submitted task synchronously and emulate a real Future: a thrown task surfaces as ExecutionException on get()
        when(intermediateBuilderExecutor.submit(any(Callable.class))).thenAnswer(invocation -> {
            Callable<Boolean> callable = invocation.getArgument(0);
            try {
                return CompletableFuture.completedFuture(callable.call());
            } catch (Exception e) {
                CompletableFuture<Boolean> failed = new CompletableFuture<>();
                failed.completeExceptionally(e);
                return failed;
            }
        });
    }

    private void stubOwningCluster(NotificationState notificationState) {
        when(dbStackOwnerIdentifierService.getOwningClusterNotificationState(CLUSTER_CRN))
                .thenReturn(new DBSourceClusterDto(CLUSTER_NAME, CLUSTER_CRN, StackType.WORKLOAD, notificationState));
    }

    private void stubEnvironmentName() {
        DetailedEnvironmentResponse env = new DetailedEnvironmentResponse();
        env.setName(ENV_NAME);
        when(environmentService.getByCrn(ENV_CRN)).thenReturn(env);
    }

    private void stubGates(boolean notificationRequiredByStatus, boolean entitlementEnabled, NotificationState clusterState) {
        lenient().when(dbStackNotificationTypePreparationService.isNotificationRequiredByStackStatus(
                ResourceEvent.REDBEAMS_EXTERNAL_DATABASE_STORAGE_LOW)).thenReturn(notificationRequiredByStatus);
        lenient().when(entitlementService.isCdpCbNotificationSendingEnabled(ACCOUNT_ID)).thenReturn(entitlementEnabled);
        stubOwningCluster(clusterState);
    }

    @Test
    void shouldSkipWhenEventIsNotNotificationTriggering() {
        stubGates(false, true, NotificationState.ENABLED);

        underTest.notify(databaseServerConfig, dbStack, DBEmailDto.storageAlert(), event);

        verifyNoInteractions(intermediateBuilderExecutor, distributionListManagementService, notificationSendingService, dataPreparationService,
                environmentService);
    }

    @Test
    void shouldSkipWhenNotificationSendingEntitlementDisabled() {
        stubGates(true, false, NotificationState.ENABLED);

        underTest.notify(databaseServerConfig, dbStack, DBEmailDto.storageAlert(), event);

        verifyNoInteractions(intermediateBuilderExecutor, distributionListManagementService, notificationSendingService, dataPreparationService,
                environmentService);
    }

    @Test
    void shouldSkipWhenOwningClusterNotificationStateDisabled() {
        stubGates(true, true, NotificationState.DISABLED);

        underTest.notify(databaseServerConfig, dbStack, DBEmailDto.storageAlert(), event);

        verifyNoInteractions(intermediateBuilderExecutor, distributionListManagementService, notificationSendingService, dataPreparationService,
                environmentService);
    }

    @Test
    void shouldRegisterDistributionListThenSendOnHappyPath() {
        setUpExecutor();
        stubGates(true, true, NotificationState.ENABLED);
        stubEnvironmentName();
        NotificationGeneratorDtos dtos = Mockito.mock(NotificationGeneratorDtos.class);
        when(dataPreparationService.notificationGeneratorDtos(eq(dbStack), any(DBSourceClusterDto.class), any(DBEmailDto.class), eq(ENV_NAME), eq(event)))
                .thenReturn(dtos);

        underTest.notify(databaseServerConfig, dbStack, DBEmailDto.storageAlert(), event);

        InOrder inOrder = Mockito.inOrder(distributionListManagementService, notificationSendingService);
        inOrder.verify(distributionListManagementService).createOrUpdateList(
                eq(ENV_CRN),
                eq(ENV_NAME),
                eq(CLUSTER_NAME),
                eq(CLUSTER_CRN),
                eq(NotificationGroupType.byCrn(CLUSTER_CRN)),
                eq(NotificationSeverity.ERROR),
                eq(DistributionListActionType.REGISTRATION));
        inOrder.verify(notificationSendingService).processAndImmediatelySend(dtos);
    }

    @Test
    void shouldDispatchSendOntoIntermediateBuilderExecutor() {
        setUpExecutor();
        stubGates(true, true, NotificationState.ENABLED);
        stubEnvironmentName();
        when(dataPreparationService.notificationGeneratorDtos(eq(dbStack), any(DBSourceClusterDto.class), any(DBEmailDto.class), eq(ENV_NAME), eq(event)))
                .thenReturn(Mockito.mock(NotificationGeneratorDtos.class));

        underTest.notify(databaseServerConfig, dbStack, DBEmailDto.storageAlert(), event);

        verify(intermediateBuilderExecutor).submit(any(Callable.class));
    }

    @Test
    void shouldSwallowWhenDistributionListRegistrationThrows() {
        setUpExecutor();
        stubGates(true, true, NotificationState.ENABLED);
        stubEnvironmentName();
        doThrow(new RuntimeException("ums down")).when(distributionListManagementService).createOrUpdateList(
                any(), any(), any(), any(), any(), any(), any());

        underTest.notify(databaseServerConfig, dbStack, DBEmailDto.storageAlert(), event);

        verify(notificationSendingService, never()).processAndImmediatelySend(any());
    }

    @Test
    void shouldSwallowWhenSendThrows() {
        setUpExecutor();
        stubGates(true, true, NotificationState.ENABLED);
        stubEnvironmentName();
        NotificationGeneratorDtos dtos = Mockito.mock(NotificationGeneratorDtos.class);
        when(dataPreparationService.notificationGeneratorDtos(eq(dbStack), any(DBSourceClusterDto.class), any(DBEmailDto.class), eq(ENV_NAME), eq(event)))
                .thenReturn(dtos);
        doThrow(new RuntimeException("notification service down")).when(notificationSendingService).processAndImmediatelySend(dtos);

        underTest.notify(databaseServerConfig, dbStack, DBEmailDto.storageAlert(), event);

        verify(notificationSendingService).processAndImmediatelySend(dtos);
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldCancelAndSwallowWhenSendTimesOut() {
        ReflectionTestUtils.setField(underTest, "emailTimeoutSeconds", 0L);
        CompletableFuture<Boolean> neverCompleting = new CompletableFuture<>();
        when(intermediateBuilderExecutor.submit(any(Callable.class))).thenReturn(neverCompleting);
        stubGates(true, true, NotificationState.ENABLED);

        underTest.notify(databaseServerConfig, dbStack, DBEmailDto.storageAlert(), event);

        assertTrue(neverCompleting.isCancelled());
    }
}
