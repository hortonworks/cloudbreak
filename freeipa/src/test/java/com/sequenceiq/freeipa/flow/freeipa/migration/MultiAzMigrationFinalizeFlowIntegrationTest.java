package com.sequenceiq.freeipa.flow.freeipa.migration;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import jakarta.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.common.exception.WebApplicationExceptionMessageExtractor;
import com.sequenceiq.cloudbreak.converter.AvailabilityZoneConverter;
import com.sequenceiq.cloudbreak.ha.NodeConfig;
import com.sequenceiq.cloudbreak.ha.service.NodeValidator;
import com.sequenceiq.cloudbreak.quartz.statuschecker.StatusCheckerConfig;
import com.sequenceiq.cloudbreak.quartz.statuschecker.service.StatusCheckerJobService;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.core.FlowEventListenerAdapter;
import com.sequenceiq.flow.core.FlowRegister;
import com.sequenceiq.flow.core.edh.FlowUsageSender;
import com.sequenceiq.flow.core.stats.FlowOperationStatisticsPersister;
import com.sequenceiq.flow.domain.FlowLog;
import com.sequenceiq.flow.repository.FlowLogRepository;
import com.sequenceiq.flow.service.FlowCancelService;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceGroupType;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceMetadataType;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceStatus;
import com.sequenceiq.freeipa.converter.cloud.CredentialToCloudCredentialConverter;
import com.sequenceiq.freeipa.converter.cloud.StackToCloudStackConverter;
import com.sequenceiq.freeipa.converter.image.ImageConverter;
import com.sequenceiq.freeipa.entity.InstanceGroup;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.entity.StackStatus;
import com.sequenceiq.freeipa.events.EventSenderService;
import com.sequenceiq.freeipa.flow.FlowIntegrationTestConfig;
import com.sequenceiq.freeipa.flow.StackStatusFinalizer;
import com.sequenceiq.freeipa.flow.freeipa.common.FreeIpaFailedFlowAnalyzer;
import com.sequenceiq.freeipa.flow.freeipa.common.FreeIpaValidationProperties;
import com.sequenceiq.freeipa.flow.freeipa.migration.action.MultiAzMigrationFinalizeActions;
import com.sequenceiq.freeipa.flow.freeipa.migration.event.MultiAzMigrationFinalizeTriggerEvent;
import com.sequenceiq.freeipa.service.CredentialService;
import com.sequenceiq.freeipa.service.DefaultRootVolumeSizeProvider;
import com.sequenceiq.freeipa.service.freeipa.flow.FreeIpaFlowManager;
import com.sequenceiq.freeipa.service.image.ImageService;
import com.sequenceiq.freeipa.service.operation.OperationService;
import com.sequenceiq.freeipa.service.stack.StackService;
import com.sequenceiq.freeipa.service.stack.StackUpdater;
import com.sequenceiq.freeipa.sync.AutoSyncConfig;
import com.sequenceiq.freeipa.sync.FreeipaJobService;

import io.micrometer.core.instrument.MeterRegistry;

@ActiveProfiles("integration-test")
@ExtendWith(SpringExtension.class)
class MultiAzMigrationFinalizeFlowIntegrationTest {
    private static final String USER_CRN = "crn:cdp:iam:us-west-1:" + UUID.randomUUID() + ":user:" + UUID.randomUUID();

    private static final long STACK_ID = 1L;

    private static final String OPERATION_ID = "opId";

    @Inject
    private FlowRegister flowRegister;

    @Inject
    private FlowLogRepository flowLogRepository;

    @Inject
    private FreeIpaFlowManager freeIpaFlowManager;

    @MockitoBean
    private StackService stackService;

    @MockitoBean
    private FlowOperationStatisticsPersister flowOperationStatisticsPersister;

    @MockitoBean
    private StackUpdater stackUpdater;

    @MockitoBean
    private OperationService operationService;

    @MockitoBean
    private NodeValidator nodeValidator;

    @MockitoBean
    private MeterRegistry meterRegistry;

    @MockitoBean
    private FlowCancelService flowCancelService;

    @MockitoBean
    private FlowUsageSender flowUsageSender;

    @MockitoBean
    private StackStatusFinalizer stackStatusFinalizer;

    @MockitoBean
    private EventSenderService eventSenderService;

    @MockitoBean
    private DefaultRootVolumeSizeProvider defaultRootVolumeSizeProvider;

    @MockitoBean
    private CloudPlatformConnectors cloudPlatformConnectors;

    @MockitoBean
    private ImageService imageService;

    @MockitoBean
    private CredentialToCloudCredentialConverter credentialToCloudCredentialConverter;

    @MockitoBean
    private CredentialService credentialService;

    @MockitoBean
    private StackToCloudStackConverter stackToCloudStackConverter;

    private Stack stack;

    @BeforeEach
    public void setup() {
        stack = new Stack();
        stack.setId(STACK_ID);
        stack.setAccountId("test-account");
        stack.setStackStatus(new StackStatus(stack, "test", DetailedStackStatus.MULTI_AZ_MIGRATION_IN_PROGRESS));

        InstanceGroup masterIg = new InstanceGroup();
        masterIg.setGroupName("master");
        masterIg.setInstanceGroupType(InstanceGroupType.MASTER);
        InstanceMetaData im0 = new InstanceMetaData();
        im0.setInstanceStatus(InstanceStatus.CREATED);
        im0.setPrivateId(0L);
        im0.setInstanceMetadataType(InstanceMetadataType.GATEWAY_PRIMARY);
        im0.setAvailabilityZone("az-1");
        masterIg.setInstanceMetaData(new HashSet<>(List.of(im0)));

        stack.setInstanceGroups(new HashSet<>(List.of(masterIg)));
        stack.setEnvironmentCrn("crn:cdp:environments:us-west-1:123456:environment:abc123");

        when(stackService.getByIdWithListsInTransaction(STACK_ID)).thenReturn(stack);
        when(stackService.getStackById(STACK_ID)).thenReturn(stack);
        doNothing().when(nodeValidator).checkForRecentHeartbeat();
    }

    @Test
    public void testSuccessfulMultiAzMigrationFinalize() {
        testFlow();

        verify(stackUpdater).updateStackStatus(eq(stack), eq(DetailedStackStatus.UPDATE_COMPLETE), anyString());
        verify(operationService).completeOperation(eq(stack.getAccountId()), eq(OPERATION_ID), any(), any());
        verify(eventSenderService).sendEventAndNotification(stack, USER_CRN,
                com.sequenceiq.cloudbreak.event.ResourceEvent.FREEIPA_MULTI_AZ_MIGRATION_FINISHED);
    }

    @Test
    public void testFailedMultiAzMigrationFinalize() {
        doThrow(new RuntimeException("stack updater blew up"))
                .when(stackUpdater).updateStackStatus(eq(stack), eq(DetailedStackStatus.UPDATE_COMPLETE), anyString());

        testFlow();

        verify(operationService).failOperation(eq(stack.getAccountId()), eq(OPERATION_ID), any());
        verify(operationService, never()).completeOperation(any(), any(), any(), any());
    }

    private FlowIdentifier triggerFlow() {
        String selector = MultiAzMigrationFinalizeFlowEvent.MULTI_AZ_MIGRATION_FINALIZE_EVENT.event();
        return ThreadBasedUserCrnProvider.doAs(
                USER_CRN,
                () -> freeIpaFlowManager.notify(selector,
                        new MultiAzMigrationFinalizeTriggerEvent(selector, STACK_ID, OPERATION_ID)));
    }

    private void letItFlow(FlowIdentifier flowIdentifier) {
        int i = 0;
        do {
            i++;
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
            }
        } while (flowRegister.get(flowIdentifier.getPollableId()) != null && i < 30);
    }

    private void flowFinishedSuccessfully() {
        ArgumentCaptor<FlowLog> flowLog = ArgumentCaptor.forClass(FlowLog.class);
        verify(flowLogRepository, times(2)).save(flowLog.capture());
        assertTrue(flowLog.getAllValues().stream().anyMatch(FlowLog::getFinalized), "flow has not finalized");
    }

    private void testFlow() {
        FlowIdentifier flowIdentifier = triggerFlow();
        letItFlow(flowIdentifier);
        flowFinishedSuccessfully();
    }

    @Profile("integration-test")
    @TestConfiguration
    @Import({
            MultiAzMigrationFinalizeFlowConfig.class,
            MultiAzMigrationFinalizeActions.class,
            FlowIntegrationTestConfig.class,
            FlowEventListenerAdapter.class,
            ImageConverter.class,
            WebApplicationExceptionMessageExtractor.class,
            FreeIpaFailedFlowAnalyzer.class,
            FreeIpaValidationProperties.class,
            NodeConfig.class,
            AvailabilityZoneConverter.class,
            FreeipaJobService.class,
            AutoSyncConfig.class,
            StatusCheckerConfig.class,
            StatusCheckerJobService.class
    })
    static class Config {
    }
}
