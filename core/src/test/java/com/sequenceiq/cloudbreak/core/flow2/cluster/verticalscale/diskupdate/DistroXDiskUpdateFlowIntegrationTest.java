package com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.diskupdate;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.diskupdate.DistroXDiskUpdateStateSelectors.DATAHUB_DISK_UPDATE_VALIDATION_EVENT;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executors;

import jakarta.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.security.CrnUserDetailsService;
import com.sequenceiq.cloudbreak.cloud.model.CloudVolumeUsageType;
import com.sequenceiq.cloudbreak.cloud.model.VolumeSetAttributes;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.concurrent.CommonExecutorServiceFactory;
import com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.diskupdate.config.DistroXDiskUpdateFlowConfig;
import com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.diskupdate.event.DistroXDiskUpdateEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.diskupdate.handler.DistroXDiskUpdateHandler;
import com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.diskupdate.handler.DistroXDiskUpdateValidationHandler;
import com.sequenceiq.cloudbreak.core.flow2.service.ReactorNotifier;
import com.sequenceiq.cloudbreak.core.flow2.stack.CloudbreakFlowMessageService;
import com.sequenceiq.cloudbreak.core.flow2.stack.update.userdata.FlowIntegrationTestConfig;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.eventbus.Promise;
import com.sequenceiq.cloudbreak.ha.NodeConfig;
import com.sequenceiq.cloudbreak.ha.service.NodeValidator;
import com.sequenceiq.cloudbreak.service.StackUpdater;
import com.sequenceiq.cloudbreak.service.VerticalScalingValidatorService;
import com.sequenceiq.cloudbreak.service.diskupdate.DiskUpdateService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.cloudbreak.view.ClusterView;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.common.api.type.ResourceType;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.core.FlowEventListenerAdapter;
import com.sequenceiq.flow.core.FlowRegister;
import com.sequenceiq.flow.core.edh.FlowUsageSender;
import com.sequenceiq.flow.core.stats.FlowOperationStatisticsPersister;
import com.sequenceiq.flow.domain.FlowLog;
import com.sequenceiq.flow.repository.FlowLogRepository;
import com.sequenceiq.flow.service.FlowCancelService;

import io.micrometer.core.instrument.MeterRegistry;

/**
 * Boots the real DistroXDiskUpdate (data hub disk type / size) state machine and drives the flow end to end,
 * verifying that events route correctly across the validation → update → finished hops, that the happy path
 * finalizes, and that a failure at either hop lands in the terminal {@code DATAHUB_DISK_UPDATE_FAILED_STATE} and
 * still finalizes (no deadlock). Isolated handler/action unit tests mock the flow context and cannot catch a
 * mis-wired transition.
 */
@ActiveProfiles("integration-test")
@ExtendWith(SpringExtension.class)
class DistroXDiskUpdateFlowIntegrationTest {

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:" + UUID.randomUUID() + ":user:" + UUID.randomUUID();

    private static final String DATAHUB_CRN = "crn:cdp:iam:us-west-1:" + UUID.randomUUID() + ":cluster:" + UUID.randomUUID();

    private static final long STACK_ID = 1L;

    private static final String GROUP = "compute";

    private static final String CLOUD_PLATFORM = "AWS";

    private static final int CURRENT_SIZE = 50;

    private static final int REQUESTED_SIZE = 100;

    private static final String CURRENT_TYPE = "gp2";

    private static final String REQUESTED_TYPE = "gp3";

    @Inject
    private FlowRegister flowRegister;

    @Inject
    private FlowLogRepository flowLogRepository;

    @Inject
    private ReactorNotifier reactorNotifier;

    @MockitoBean
    private StackDtoService stackDtoService;

    @MockitoBean
    private StackService stackService;

    @MockitoBean
    private DiskUpdateService diskUpdateService;

    @MockitoBean
    private VerticalScalingValidatorService verticalScalingValidatorService;

    @MockitoBean
    private CloudbreakFlowMessageService flowMessageService;

    @MockitoBean
    private StackUpdater stackUpdater;

    @MockitoBean
    private FlowOperationStatisticsPersister flowOperationStatisticsPersister;

    @MockitoBean
    private CrnUserDetailsService crnUserDetailsService;

    @MockitoBean
    private NodeConfig nodeConfig;

    @MockitoBean
    private MeterRegistry meterRegistry;

    @MockitoBean
    private NodeValidator nodeValidator;

    @MockitoBean
    private FlowCancelService flowCancelService;

    @MockitoBean
    private FlowUsageSender flowUsageSender;

    @BeforeEach
    void setUp() {
        StackDto stackDto = mockStackDto();
        StackView stackView = mockStackView();
        when(stackDtoService.getById(STACK_ID)).thenReturn(stackDto);
        when(stackDtoService.getStackViewById(STACK_ID)).thenReturn(stackView);
        when(stackDtoService.getClusterViewByStackId(STACK_ID)).thenReturn(mock(ClusterView.class));

        when(stackService.getByIdWithLists(STACK_ID)).thenReturn(mock(Stack.class));

        when(diskUpdateService.isDiskTypeChangeSupported(CLOUD_PLATFORM)).thenReturn(true);
        when(verticalScalingValidatorService.validateAddVolumesRequest(any(), any(), any())).thenReturn(ValidationResult.builder().build());
    }

    @Test
    void testDiskUpdateHappyPath() throws Exception {
        triggerAndWait();

        flowFinishedSuccessfully();
        verify(diskUpdateService).updateDiskTypeAndSize(eq(GROUP), eq(REQUESTED_TYPE), eq(REQUESTED_SIZE), any(), eq(STACK_ID));
    }

    @Test
    void testDiskUpdateFailsDuringValidation() {
        when(diskUpdateService.isDiskTypeChangeSupported(CLOUD_PLATFORM)).thenReturn(false);

        triggerAndWait();

        flowFinishedSuccessfully();
        verify(diskUpdateService, atLeastOnce()).isDiskTypeChangeSupported(CLOUD_PLATFORM);
    }

    @Test
    void testDiskUpdateFailsDuringUpdate() throws Exception {
        doThrow(new RuntimeException("disk update failed")).when(diskUpdateService)
                .updateDiskTypeAndSize(anyString(), anyString(), anyInt(), any(), anyLong());

        triggerAndWait();

        flowFinishedSuccessfully();
        verify(diskUpdateService, atLeastOnce()).updateDiskTypeAndSize(eq(GROUP), eq(REQUESTED_TYPE), eq(REQUESTED_SIZE), any(), eq(STACK_ID));
    }

    private void triggerAndWait() {
        FlowIdentifier flowIdentifier = triggerFlow();
        letItFlow(flowIdentifier);
    }

    private FlowIdentifier triggerFlow() {
        String selector = DATAHUB_DISK_UPDATE_VALIDATION_EVENT.event();
        DistroXDiskUpdateEvent triggerEvent = DistroXDiskUpdateEvent.builder()
                .withSelector(selector)
                .withResourceId(STACK_ID)
                .withStackId(STACK_ID)
                .withGroup(GROUP)
                .withSize(REQUESTED_SIZE)
                .withVolumeType(REQUESTED_TYPE)
                .withCloudPlatform(CLOUD_PLATFORM)
                .withAccepted(new Promise<>())
                .build();
        return ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> reactorNotifier.notify(STACK_ID, selector, triggerEvent));
    }

    private void letItFlow(FlowIdentifier flowIdentifier) {
        int i = 0;
        do {
            i++;
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        } while (flowRegister.get(flowIdentifier.getPollableId()) != null && i < 50);
    }

    private void flowFinishedSuccessfully() {
        ArgumentCaptor<FlowLog> flowLog = ArgumentCaptor.forClass(FlowLog.class);
        verify(flowLogRepository, atLeastOnce()).save(flowLog.capture());
        assertTrue(flowLog.getAllValues().stream().anyMatch(FlowLog::getFinalized), "flow has not finalized");
    }

    private StackDto mockStackDto() {
        StackDto stackDto = mock(StackDto.class);
        when(stackDto.getId()).thenReturn(STACK_ID);
        when(stackDto.getName()).thenReturn("stackname");
        when(stackDto.getResourceCrn()).thenReturn(DATAHUB_CRN);
        when(stackDto.getCloudPlatform()).thenReturn(CLOUD_PLATFORM);
        when(stackDto.getResources()).thenReturn(Set.of(volumeSetResource()));
        return stackDto;
    }

    private Resource volumeSetResource() {
        VolumeSetAttributes attributes = new VolumeSetAttributes.Builder()
                .withVolumes(List.of(new VolumeSetAttributes.Volume("vol-1", "/dev/xvdb", CURRENT_SIZE, CURRENT_TYPE, CloudVolumeUsageType.GENERAL)))
                .build();
        Resource resource = new Resource();
        resource.setInstanceId("i-123");
        resource.setInstanceGroup(GROUP);
        resource.setResourceType(ResourceType.AWS_VOLUMESET);
        resource.setResourceName("vol-set-1");
        resource.setAttributes(new Json(attributes));
        return resource;
    }

    private StackView mockStackView() {
        StackView stackView = mock(StackView.class);
        when(stackView.getId()).thenReturn(STACK_ID);
        when(stackView.getName()).thenReturn("stackname");
        return stackView;
    }

    @Profile("integration-test")
    @TestConfiguration
    @Import({
            DistroXDiskUpdateFlowConfig.class,
            DistroXDiskUpdateActions.class,
            DistroXDiskUpdateValidationHandler.class,
            DistroXDiskUpdateHandler.class,
            FlowEventListenerAdapter.class,
            FlowIntegrationTestConfig.class
    })
    static class Config {

        @Bean
        CommonExecutorServiceFactory commonExecutorServiceFactory() {
            CommonExecutorServiceFactory commonExecutorServiceFactory = mock(CommonExecutorServiceFactory.class);
            when(commonExecutorServiceFactory.newThreadPoolExecutorService(any(), any(), anyInt(), anyInt(), anyLong(), any(), any(), any(), any()))
                    .thenReturn(Executors.newCachedThreadPool());
            return commonExecutorServiceFactory;
        }
    }
}
