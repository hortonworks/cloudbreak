package com.sequenceiq.cloudbreak.core.flow2.cluster.addvolumes;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.addvolumes.AddVolumesEvent.ADD_VOLUMES_TRIGGER_EVENT;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.ADDING_VOLUMES_FAILED;
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
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executors;

import jakarta.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import com.sequenceiq.cloudbreak.cluster.util.ResourceAttributeUtil;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessorFactory;
import com.sequenceiq.cloudbreak.concurrent.CommonExecutorServiceFactory;
import com.sequenceiq.cloudbreak.converter.spi.StackToCloudStackConverter;
import com.sequenceiq.cloudbreak.core.flow2.AbstractFlowIntegrationTest;
import com.sequenceiq.cloudbreak.core.flow2.cluster.addvolumes.actions.AddVolumesActions;
import com.sequenceiq.cloudbreak.core.flow2.cluster.addvolumes.event.AddVolumesRequest;
import com.sequenceiq.cloudbreak.core.flow2.cluster.addvolumes.handler.AddVolumesCMConfigHandler;
import com.sequenceiq.cloudbreak.core.flow2.cluster.addvolumes.handler.AddVolumesHandler;
import com.sequenceiq.cloudbreak.core.flow2.cluster.addvolumes.handler.AddVolumesOrchestrationHandler;
import com.sequenceiq.cloudbreak.core.flow2.cluster.addvolumes.handler.AddVolumesValidateHandler;
import com.sequenceiq.cloudbreak.core.flow2.cluster.addvolumes.handler.AttachVolumesHandler;
import com.sequenceiq.cloudbreak.core.flow2.service.AddVolumesService;
import com.sequenceiq.cloudbreak.core.flow2.service.ReactorNotifier;
import com.sequenceiq.cloudbreak.core.flow2.stack.CloudbreakFlowMessageService;
import com.sequenceiq.cloudbreak.core.flow2.stack.update.userdata.FlowIntegrationTestConfig;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.ha.NodeConfig;
import com.sequenceiq.cloudbreak.ha.service.NodeValidator;
import com.sequenceiq.cloudbreak.service.ConfigUpdateUtilService;
import com.sequenceiq.cloudbreak.service.StackUpdater;
import com.sequenceiq.cloudbreak.service.diskupdate.DiskUpdateService;
import com.sequenceiq.cloudbreak.service.resource.ResourceService;
import com.sequenceiq.cloudbreak.service.stack.InstanceGroupService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.template.TemplateService;
import com.sequenceiq.cloudbreak.util.StackUtil;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.cloudbreak.workspace.model.Tenant;
import com.sequenceiq.common.api.type.ResourceType;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.core.FlowEventListenerAdapter;
import com.sequenceiq.flow.core.edh.FlowUsageSender;
import com.sequenceiq.flow.core.stats.FlowOperationStatisticsPersister;
import com.sequenceiq.flow.service.FlowCancelService;

import io.micrometer.core.instrument.MeterRegistry;

/**
 * Boots the real AddVolumes (attach) state machine and drives the flow end to end, verifying that events route
 * correctly across the validate → add → attach → orchestration → CM-config hops, that the happy path finalizes,
 * and that a failure at any hop lands in the terminal {@code ADD_VOLUMES_FAILED_STATE} and still finalizes (no
 * deadlock). Isolated handler/action unit tests mock the flow context and cannot catch a mis-wired transition.
 */
@ActiveProfiles("integration-test")
@ExtendWith(SpringExtension.class)
class AddVolumesFlowIntegrationTest extends AbstractFlowIntegrationTest {

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:" + UUID.randomUUID() + ":user:" + UUID.randomUUID();

    private static final String DATAHUB_CRN = "crn:cdp:iam:us-west-1:" + UUID.randomUUID() + ":cluster:" + UUID.randomUUID();

    private static final long STACK_ID = 1L;

    private static final String GROUP = "compute";

    private static final String CLOUD_PLATFORM = "AWS";

    private static final long NUMBER_OF_DISKS = 2L;

    private static final long SIZE = 100L;

    private static final String TYPE = "gp2";

    @Inject
    private ReactorNotifier reactorNotifier;

    @MockitoBean
    private StackDtoService stackDtoService;

    @MockitoBean
    private StackToCloudStackConverter cloudStackConverter;

    @MockitoBean
    private StackUtil stackUtil;

    @MockitoBean
    private AddVolumesService addVolumesService;

    @MockitoBean
    private StackService stackService;

    @MockitoBean
    private ResourceService resourceService;

    @MockitoBean
    private InstanceGroupService instanceGroupService;

    @MockitoBean
    private TemplateService templateService;

    @MockitoBean
    private ResourceAttributeUtil resourceAttributeUtil;

    @MockitoBean
    private DiskUpdateService diskUpdateService;

    @MockitoBean
    private ConfigUpdateUtilService configUpdateUtilService;

    @MockitoBean
    private CmTemplateProcessorFactory cmTemplateProcessorFactory;

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

    private CmTemplateProcessor processor;

    @BeforeEach
    void setUp() {
        StackDto stackDto = mockStackDto();
        StackView stackView = mockStackView();
        when(stackDtoService.getById(STACK_ID)).thenReturn(stackDto);
        when(stackDtoService.getStackViewById(STACK_ID)).thenReturn(stackView);

        Stack stack = mock(Stack.class);
        when(stack.getDiskResourceType()).thenReturn(ResourceType.AWS_VOLUMESET);
        when(stackService.getById(STACK_ID)).thenReturn(stack);
        when(stackService.getByIdWithLists(STACK_ID)).thenReturn(stack);
        when(resourceService.findAllByStackIdAndInstanceGroupAndResourceTypeIn(eq(STACK_ID), eq(GROUP), any())).thenReturn(List.of());

        when(addVolumesService.createVolumes(any(), any(), anyInt(), anyString(), anyLong())).thenReturn(List.of());

        when(instanceGroupService.findInstanceGroupViewByStackIdAndGroupName(STACK_ID, GROUP)).thenReturn(Optional.empty());

        processor = mock(CmTemplateProcessor.class);
        when(cmTemplateProcessorFactory.get(any())).thenReturn(processor);
        when(processor.getServiceComponentsByHostGroup()).thenReturn(Map.of(GROUP, Set.of()));
        when(processor.getHostTemplateRoleNames(GROUP)).thenReturn(List.of());
    }

    @Test
    void testAddVolumesHappyPath() throws Exception {
        triggerAndWait();

        flowFinishedSuccessfully();
        verify(addVolumesService).validateVolumeAddition(eq(STACK_ID), eq(GROUP), any());
        verify(addVolumesService).createVolumes(any(), any(), anyInt(), anyString(), anyLong());
        verify(addVolumesService).attachVolumes(any(), eq(STACK_ID));
        verify(addVolumesService).redeployStatesAndMountDisks(any(), eq(GROUP));
        verify(configUpdateUtilService).updateCMConfigsForComputeAndStartServices(any(), any(), any(), eq(GROUP));
    }

    @Test
    void testAddVolumesFailsDuringValidation() {
        doThrow(new RuntimeException("validation failed")).when(addVolumesService).validateVolumeAddition(anyLong(), anyString(), any());

        triggerAndWait();

        flowFinalized();
        verify(flowMessageService).fireEventAndLog(eq(STACK_ID), eq(ADDING_VOLUMES_FAILED.name()), eq(ADDING_VOLUMES_FAILED), anyString());
        verify(addVolumesService, atLeastOnce()).validateVolumeAddition(eq(STACK_ID), eq(GROUP), any());
    }

    @Test
    void testAddVolumesFailsDuringCreate() {
        doThrow(new RuntimeException("create failed")).when(addVolumesService).createVolumes(any(), any(), anyInt(), anyString(), anyLong());

        triggerAndWait();

        flowFinalized();
        verify(flowMessageService).fireEventAndLog(eq(STACK_ID), eq(ADDING_VOLUMES_FAILED.name()), eq(ADDING_VOLUMES_FAILED), anyString());
        verify(addVolumesService, atLeastOnce()).createVolumes(any(), any(), anyInt(), anyString(), anyLong());
    }

    @Test
    void testAddVolumesFailsDuringAttach() {
        doThrow(new RuntimeException("attach failed")).when(addVolumesService).attachVolumes(any(), anyLong());

        triggerAndWait();

        flowFinalized();
        verify(flowMessageService).fireEventAndLog(eq(STACK_ID), eq(ADDING_VOLUMES_FAILED.name()), eq(ADDING_VOLUMES_FAILED), anyString());
        verify(addVolumesService, atLeastOnce()).attachVolumes(any(), eq(STACK_ID));
    }

    @Test
    void testAddVolumesFailsDuringOrchestration() throws Exception {
        doThrow(new RuntimeException("orchestration failed")).when(addVolumesService).redeployStatesAndMountDisks(any(), anyString());

        triggerAndWait();

        flowFinalized();
        verify(flowMessageService).fireEventAndLog(eq(STACK_ID), eq(ADDING_VOLUMES_FAILED.name()), eq(ADDING_VOLUMES_FAILED), anyString());
        verify(addVolumesService, atLeastOnce()).redeployStatesAndMountDisks(any(), eq(GROUP));
    }

    @Test
    void testAddVolumesFailsDuringCmConfig() {
        doThrow(new RuntimeException("cm config failed")).when(configUpdateUtilService)
                .updateCMConfigsForComputeAndStartServices(any(), any(), any(), anyString());

        triggerAndWait();

        flowFinalized();
        verify(flowMessageService).fireEventAndLog(eq(STACK_ID), eq(ADDING_VOLUMES_FAILED.name()), eq(ADDING_VOLUMES_FAILED), anyString());
        verify(configUpdateUtilService, atLeastOnce()).updateCMConfigsForComputeAndStartServices(any(), any(), any(), eq(GROUP));
    }

    private void triggerAndWait() {
        FlowIdentifier flowIdentifier = triggerFlow();
        letItFlow(flowIdentifier);
    }

    private FlowIdentifier triggerFlow() {
        String selector = ADD_VOLUMES_TRIGGER_EVENT.event();
        AddVolumesRequest request = AddVolumesRequest.Builder.builder()
                .withSelector(selector)
                .withStackId(STACK_ID)
                .withNumberOfDisks(NUMBER_OF_DISKS)
                .withType(TYPE)
                .withSize(SIZE)
                .withCloudVolumeUsageType(CloudVolumeUsageType.GENERAL)
                .withInstanceGroup(GROUP)
                .build();
        return ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> reactorNotifier.notify(STACK_ID, selector, request));
    }

    private StackDto mockStackDto() {
        StackDto stackDto = mock(StackDto.class);
        Tenant tenant = new Tenant();
        tenant.setId(1L);
        when(stackDto.getId()).thenReturn(STACK_ID);
        when(stackDto.getName()).thenReturn("stackname");
        when(stackDto.getResourceCrn()).thenReturn(DATAHUB_CRN);
        when(stackDto.getRegion()).thenReturn("us-west-1");
        when(stackDto.getAvailabilityZone()).thenReturn("us-west-1a");
        when(stackDto.getCloudPlatform()).thenReturn(CLOUD_PLATFORM);
        when(stackDto.getPlatformVariant()).thenReturn(CLOUD_PLATFORM);
        when(stackDto.getEnvironmentCrn()).thenReturn("envCrn");
        when(stackDto.getBlueprintJsonText()).thenReturn("{}");
        when(stackDto.getWorkspaceId()).thenReturn(1L);
        when(stackDto.getTenant()).thenReturn(tenant);
        return stackDto;
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
            AddVolumesFlowConfig.class,
            AddVolumesActions.class,
            AddVolumesValidateHandler.class,
            AddVolumesHandler.class,
            AttachVolumesHandler.class,
            AddVolumesOrchestrationHandler.class,
            AddVolumesCMConfigHandler.class,
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
