package com.sequenceiq.cloudbreak.core.flow2.cluster.deletevolumes;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.DELETE_FAILED;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.deletevolumes.DeleteVolumesEvent.DELETE_VOLUMES_VALIDATION_EVENT;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_DELETE_VOLUMES_FAILED;
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

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackDeleteVolumesRequest;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.security.CrnUserDetailsService;
import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.VolumeSetAttributes;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessorFactory;
import com.sequenceiq.cloudbreak.concurrent.CommonExecutorServiceFactory;
import com.sequenceiq.cloudbreak.converter.spi.ResourceToCloudResourceConverter;
import com.sequenceiq.cloudbreak.core.flow2.AbstractFlowIntegrationTest;
import com.sequenceiq.cloudbreak.core.flow2.cluster.deletevolumes.handler.DeleteVolumesCMConfigHandler;
import com.sequenceiq.cloudbreak.core.flow2.cluster.deletevolumes.handler.DeleteVolumesHandler;
import com.sequenceiq.cloudbreak.core.flow2.cluster.deletevolumes.handler.DeleteVolumesUnmountHandler;
import com.sequenceiq.cloudbreak.core.flow2.cluster.deletevolumes.handler.DeleteVolumesValidationHandler;
import com.sequenceiq.cloudbreak.core.flow2.event.DeleteVolumesTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.service.ReactorNotifier;
import com.sequenceiq.cloudbreak.core.flow2.stack.CloudbreakFlowMessageService;
import com.sequenceiq.cloudbreak.core.flow2.stack.update.userdata.FlowIntegrationTestConfig;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.ha.NodeConfig;
import com.sequenceiq.cloudbreak.ha.service.NodeValidator;
import com.sequenceiq.cloudbreak.service.ConfigUpdateUtilService;
import com.sequenceiq.cloudbreak.service.StackUpdater;
import com.sequenceiq.cloudbreak.service.resource.ResourceService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.util.StackUtil;
import com.sequenceiq.cloudbreak.view.ClusterView;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.cloudbreak.workspace.model.Tenant;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;
import com.sequenceiq.common.api.type.ResourceType;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.core.FlowEventListenerAdapter;
import com.sequenceiq.flow.core.edh.FlowUsageSender;
import com.sequenceiq.flow.core.stats.FlowOperationStatisticsPersister;
import com.sequenceiq.flow.service.FlowCancelService;

import io.micrometer.core.instrument.MeterRegistry;

/**
 * Boots the real DeleteVolumes state machine and drives the flow end to end, verifying that events route correctly
 * between states, that the happy path finalizes, and that a failure at any hop lands in the terminal
 * {@code DELETE_VOLUMES_FAILED_STATE} and still finalizes (no deadlock). The failure-path coverage guards the
 * flow's use of {@code DELETE_VOLUMES_FAIL_HANDLED_EVENT} directly as each transition's failure event.
 */
@ActiveProfiles("integration-test")
@ExtendWith(SpringExtension.class)
class DeleteVolumesFlowIntegrationTest extends AbstractFlowIntegrationTest {

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:" + UUID.randomUUID() + ":user:" + UUID.randomUUID();

    private static final String DATAHUB_CRN = "crn:cdp:iam:us-west-1:" + UUID.randomUUID() + ":cluster:" + UUID.randomUUID();

    private static final long STACK_ID = 1L;

    private static final String GROUP = "compute";

    private static final String CLOUD_PLATFORM = "AWS";

    @Inject
    private ReactorNotifier reactorNotifier;

    @MockitoBean
    private StackDtoService stackDtoService;

    @MockitoBean
    private StackService stackService;

    @MockitoBean
    private ResourceService resourceService;

    @MockitoBean
    private DeleteVolumesService deleteVolumesService;

    @MockitoBean
    private CmTemplateProcessorFactory cmTemplateProcessorFactory;

    @MockitoBean
    private ResourceToCloudResourceConverter cloudResourceConverter;

    @MockitoBean
    private ConfigUpdateUtilService configUpdateUtilService;

    @MockitoBean
    private CloudPlatformConnectors cloudPlatformConnectors;

    @MockitoBean
    private StackUtil stackUtil;

    @MockitoBean
    private StackUpdater stackUpdater;

    @MockitoBean
    private CloudbreakFlowMessageService flowMessageService;

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
        when(stackDtoService.getClusterViewByStackId(STACK_ID)).thenReturn(mock(ClusterView.class));

        Stack stack = mockStack();
        when(stackService.getByIdWithLists(STACK_ID)).thenReturn(stack);
        when(resourceService.findAllByStackIdAndResourceTypeIn(eq(STACK_ID), any())).thenReturn(List.of());

        processor = mock(CmTemplateProcessor.class);
        when(cmTemplateProcessorFactory.get(any())).thenReturn(processor);
        when(processor.getComponentsInHostGroup(GROUP)).thenReturn(Set.of());
        when(processor.getServiceComponentsByHostGroup()).thenReturn(Map.of(GROUP, Set.of()));
        when(processor.getHostTemplateRoleNames(GROUP)).thenReturn(List.of());

        CloudResource cloudResource = mock(CloudResource.class);
        VolumeSetAttributes volumeSetAttributes = mock(VolumeSetAttributes.class);
        VolumeSetAttributes.Volume volume = mock(VolumeSetAttributes.Volume.class);
        when(volumeSetAttributes.getVolumes()).thenReturn(List.of(volume));
        when(cloudResource.getParameter(CloudResource.ATTRIBUTES, VolumeSetAttributes.class)).thenReturn(volumeSetAttributes);
        when(cloudResourceConverter.convert(any())).thenReturn(cloudResource);

        CloudConnector cloudConnector = mock(CloudConnector.class);
        AuthenticatedContext authenticatedContext = mock(AuthenticatedContext.class);
        when(cloudPlatformConnectors.get(any())).thenReturn(cloudConnector);
        when(cloudConnector.authentication()).thenReturn(mock(com.sequenceiq.cloudbreak.cloud.Authenticator.class));
        when(cloudConnector.authentication().authenticate(any(), any())).thenReturn(authenticatedContext);
    }

    @Test
    void testDeleteVolumesHappyPath() throws Exception {
        triggerAndWait();

        flowFinishedSuccessfully();
        verify(deleteVolumesService).unmountBlockStorageDisks(any(), eq(GROUP));
        verify(deleteVolumesService).detachResources(any(), any(), any());
        verify(deleteVolumesService).deleteResources(any(), any(), any());
        verify(deleteVolumesService).deleteVolumeResources(any(), any());
        verify(deleteVolumesService).startClouderaManagerService(any(), any());
    }

    @Test
    void testDeleteVolumesFailsDuringUnmount() throws Exception {
        doThrow(new RuntimeException("unmount failed")).when(deleteVolumesService).unmountBlockStorageDisks(any(), anyString());

        triggerAndWait();

        flowFinalized();
        verify(flowMessageService).fireEventAndLog(eq(STACK_ID), eq(DELETE_FAILED.name()), eq(CLUSTER_DELETE_VOLUMES_FAILED), anyString());
        verify(deleteVolumesService, atLeastOnce()).unmountBlockStorageDisks(any(), eq(GROUP));
    }

    @Test
    void testDeleteVolumesFailsDuringDelete() throws Exception {
        doThrow(new RuntimeException("delete failed")).when(deleteVolumesService).deleteResources(any(), any(), any());

        triggerAndWait();

        flowFinalized();
        verify(flowMessageService).fireEventAndLog(eq(STACK_ID), eq(DELETE_FAILED.name()), eq(CLUSTER_DELETE_VOLUMES_FAILED), anyString());
        verify(deleteVolumesService, atLeastOnce()).deleteResources(any(), any(), any());
    }

    @Test
    void testDeleteVolumesFailsDuringCmConfig() throws Exception {
        doThrow(new RuntimeException("cm config failed")).when(deleteVolumesService).startClouderaManagerService(any(), any());

        triggerAndWait();

        flowFinalized();
        verify(flowMessageService).fireEventAndLog(eq(STACK_ID), eq(DELETE_FAILED.name()), eq(CLUSTER_DELETE_VOLUMES_FAILED), anyString());
        verify(deleteVolumesService, atLeastOnce()).startClouderaManagerService(any(), any());
    }

    private void triggerAndWait() {
        FlowIdentifier flowIdentifier = triggerFlow();
        letItFlow(flowIdentifier);
    }

    private FlowIdentifier triggerFlow() {
        String selector = DELETE_VOLUMES_VALIDATION_EVENT.event();
        StackDeleteVolumesRequest request = new StackDeleteVolumesRequest();
        request.setGroup(GROUP);
        request.setStackId(STACK_ID);
        DeleteVolumesTriggerEvent triggerEvent = DeleteVolumesTriggerEvent.Builder.anBuilder()
                .withSelector(selector)
                .withStackId(STACK_ID)
                .withStackDeleteVolumesRequest(request)
                .build();
        return ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> reactorNotifier.notify(STACK_ID, selector, triggerEvent));
    }

    private StackDto mockStackDto() {
        StackDto stackDto = mock(StackDto.class);
        Workspace workspace = new Workspace();
        workspace.setId(1L);
        workspace.setTenant(new Tenant());
        Resource volumeSetResource = new Resource();
        volumeSetResource.setInstanceGroup(GROUP);
        volumeSetResource.setResourceType(ResourceType.AWS_VOLUMESET);
        volumeSetResource.setResourceName("vol-1");
        when(stackDto.getId()).thenReturn(STACK_ID);
        when(stackDto.getName()).thenReturn("stackname");
        when(stackDto.getResourceCrn()).thenReturn(DATAHUB_CRN);
        when(stackDto.getRegion()).thenReturn("us-west-1");
        when(stackDto.getAvailabilityZone()).thenReturn("us-west-1a");
        when(stackDto.getCloudPlatform()).thenReturn(CLOUD_PLATFORM);
        when(stackDto.getPlatformVariant()).thenReturn(CLOUD_PLATFORM);
        when(stackDto.getEnvironmentCrn()).thenReturn("envCrn");
        when(stackDto.getBlueprintJsonText()).thenReturn("{}");
        when(stackDto.getWorkspace()).thenReturn(workspace);
        when(stackDto.getResources()).thenReturn(Set.of(volumeSetResource));
        return stackDto;
    }

    private StackView mockStackView() {
        StackView stackView = mock(StackView.class);
        when(stackView.getId()).thenReturn(STACK_ID);
        return stackView;
    }

    private Stack mockStack() {
        Stack stack = mock(Stack.class);
        when(stack.getDiskResourceType()).thenReturn(ResourceType.AWS_VOLUMESET);
        return stack;
    }

    @Profile("integration-test")
    @TestConfiguration
    @Import({
            DeleteVolumesFlowConfig.class,
            DeleteVolumesActions.class,
            DeleteVolumesValidationHandler.class,
            DeleteVolumesUnmountHandler.class,
            DeleteVolumesHandler.class,
            DeleteVolumesCMConfigHandler.class,
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
