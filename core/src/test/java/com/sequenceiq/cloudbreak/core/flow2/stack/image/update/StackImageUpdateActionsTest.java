package com.sequenceiq.cloudbreak.core.flow2.stack.image.update;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.statemachine.ExtendedState;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.state.State;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformRequest;
import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult;
import com.sequenceiq.cloudbreak.cloud.event.resource.UpdateImageRequest;
import com.sequenceiq.cloudbreak.cloud.event.setup.PrepareImageRequest;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.converter.spi.ResourceToCloudResourceConverter;
import com.sequenceiq.cloudbreak.converter.spi.StackToCloudStackConverter;
import com.sequenceiq.cloudbreak.core.CloudbreakImageCatalogException;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.core.flow2.CheckResult;
import com.sequenceiq.cloudbreak.core.flow2.event.StackImageUpdateTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.stack.AbstractStackFailureAction;
import com.sequenceiq.cloudbreak.core.flow2.stack.CloudbreakFlowMessageService;
import com.sequenceiq.cloudbreak.core.flow2.stack.StackFailureContext;
import com.sequenceiq.cloudbreak.core.flow2.stack.provision.service.StackCreationService;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.ImageUpdateEvent;
import com.sequenceiq.cloudbreak.reactor.handler.ImageFallbackService;
import com.sequenceiq.cloudbreak.service.ComponentConfigProviderService;
import com.sequenceiq.cloudbreak.service.StackUpdater;
import com.sequenceiq.cloudbreak.service.image.ImageService;
import com.sequenceiq.cloudbreak.service.image.StatedImage;
import com.sequenceiq.cloudbreak.service.resource.ResourceService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.service.stack.StackImageService;
import com.sequenceiq.cloudbreak.service.upgrade.ImageComponentUpdaterService;
import com.sequenceiq.cloudbreak.util.StackUtil;
import com.sequenceiq.cloudbreak.workspace.model.Tenant;
import com.sequenceiq.cloudbreak.workspace.model.User;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;
import com.sequenceiq.common.api.type.ResourceType;
import com.sequenceiq.flow.core.Flow;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.core.FlowRegister;
import com.sequenceiq.flow.core.MessageFactory.HEADERS;
import com.sequenceiq.flow.reactor.ErrorHandlerAwareReactorEventFactory;
import com.sequenceiq.flow.service.flowlog.FlowLogDBService;

@ExtendWith(MockitoExtension.class)
class StackImageUpdateActionsTest {

    private static final String EVENT_NAME = "eventName";

    @Mock
    private CloudbreakFlowMessageService flowMessageService;

    @Mock
    private StackDtoService stackDtoService;

    @Mock
    private StackCreationService stackCreationService;

    @Mock
    private StackUtil stackUtil;

    @Mock
    private CloudCredential cloudCredential;

    @Mock
    private StackToCloudStackConverter cloudStackConverter;

    @Mock
    private StackImageUpdateService stackImageUpdateService;

    @Mock
    private StackImageService stackImageService;

    @Mock
    private FlowRegister runningFlows;

    @Mock
    private EventBus eventBus;

    @Mock
    private ErrorHandlerAwareReactorEventFactory reactorEventFactory;

    @Mock
    private ImageService imageService;

    @Mock
    private ResourceService resourceService;

    @Mock
    private ResourceToCloudResourceConverter resourceToCloudResourceConverter;

    @Mock
    private ComponentConfigProviderService componentConfigProviderService;

    @Mock
    private StackUpdater stackUpdater;

    @Mock
    private StateContext stateContext;

    @Mock
    private ExtendedState extendedState;

    @Mock
    private StateMachine stateMachine;

    @Mock
    private State state;

    @Mock
    private StatedImage statedImage;

    @Mock
    private Image image;

    @Mock
    private Flow flow;

    @Mock
    private StackFailureContext failureContext;

    @Mock
    private ImageFallbackService imageFallbackService;

    @Mock
    private ImageComponentUpdaterService imageComponentUpdaterService;

    @Mock
    private FlowLogDBService flowLogDBService;

    @InjectMocks
    private final AbstractStackImageUpdateAction<?> checkImageAction = spy(new StackImageUpdateActions().checkImageVersion());

    @InjectMocks
    private final AbstractStackImageUpdateAction<?> checkPackageVersionsAction = spy(new StackImageUpdateActions().checkPackageVersions());

    @InjectMocks
    private final AbstractStackImageUpdateAction<?> updateImageAction = spy(new StackImageUpdateActions().updateImage());

    @InjectMocks
    private final AbstractStackImageUpdateAction<?> prepareImageAction = spy(new StackImageUpdateActions().prepareImageAction());

    @InjectMocks
    private final AbstractStackImageUpdateAction<?> setImageAction = spy(new StackImageUpdateActions().setImageAction());

    @InjectMocks
    private final AbstractStackImageUpdateAction<?> finishAction = spy(new StackImageUpdateActions().finishAction());

    @InjectMocks
    private final AbstractStackFailureAction<?, ?> handleImageUpdateFailureAction = spy(new StackImageUpdateActions().handleImageUpdateFailure());

    private final Map<Object, Object> variables = new HashMap<>();

    @BeforeEach
    void setup() throws CloudbreakImageNotFoundException {
        when(stateContext.getMessageHeader(HEADERS.FLOW_PARAMETERS.name())).thenReturn(new FlowParameters("flowId", "usercrn"));
        when(stateContext.getExtendedState()).thenReturn(extendedState);
        when(stateContext.getStateMachine()).thenReturn(stateMachine);
        when(stateMachine.getState()).thenReturn(state);
        when(extendedState.getVariables()).thenReturn(variables);
        when(runningFlows.getFlowChainId(anyString())).thenReturn("flowchainid");
        when(reactorEventFactory.createEvent(any(Map.class), any(Object.class))).thenReturn(new Event("dummy"));
        lenient().when(imageService.getImage(anyLong())).thenReturn(image);

        User user = new User();
        user.setUserId("horton@hortonworks.com");
        user.setUserCrn("testCrn");
        user.setUserName("Alma ur");
        Tenant tenant = new Tenant();
        tenant.setName("hortonworks");
        tenant.setId(1L);
        Workspace workspace = new Workspace();
        workspace.setId(1L);
        workspace.setTenant(tenant);
        Stack stack = new Stack();
        stack.setCreator(user);
        stack.setWorkspace(workspace);
        stack.setId(1L);
        stack.setRegion("region");
        stack.setAvailabilityZone("az");
        stack.setResourceCrn("crn:cdp:datalake:us-west-1:tenant:cluster:1234");
        StackDto stackDto = spy(StackDto.class);
        lenient().when(stackDto.getStack()).thenReturn(stack);
        lenient().when(stackDto.getWorkspace()).thenReturn(workspace);
        lenient().when(stackDto.getTenant()).thenReturn(tenant);
        lenient().when(stackDtoService.getById(anyLong())).thenReturn(stackDto);
        lenient().when(stackDtoService.getStackViewById(anyLong())).thenReturn(stack);
        lenient().when(stackUtil.getCloudCredential(any())).thenReturn(cloudCredential);

        variables.clear();
    }

    @Test
    void checkImageVersion() {
        StackImageUpdateTriggerEvent payload = new StackImageUpdateTriggerEvent(StackImageUpdateEvent.STACK_IMAGE_UPDATE_EVENT.event(), 1L, "imageId");
        when(stateContext.getMessageHeader(HEADERS.DATA.name())).thenReturn(payload);
        when(state.getId()).thenReturn(StackImageUpdateState.CHECK_IMAGE_VERSIONS_STATE);
        when(stackImageUpdateService.isCbVersionOk(any(StackDto.class))).thenReturn(true);

        checkImageAction.execute(stateContext);

        verify(flowMessageService, times(1)).fireEventAndLog(anyLong(), eq(Status.UPDATE_IN_PROGRESS.name()),
                eq(ResourceEvent.STACK_IMAGE_UPDATE_STARTED));
        verify(stackImageUpdateService, times(1)).getNewImageIfVersionsMatch(any(StackDto.class), anyString(), eq(null), eq(null));
        verify(eventBus, times(1)).notify(eq(StackImageUpdateEvent.CHECK_IMAGE_VERESIONS_FINISHED_EVENT.event()), any(Event.class));
    }

    @Test
    void checkImageVersionNotOk() {
        StackImageUpdateTriggerEvent payload = new StackImageUpdateTriggerEvent(StackImageUpdateEvent.STACK_IMAGE_UPDATE_EVENT.event(), 1L, "imageId");
        when(stateContext.getMessageHeader(HEADERS.DATA.name())).thenReturn(payload);
        when(state.getId()).thenReturn(StackImageUpdateState.CHECK_IMAGE_VERSIONS_STATE);
        when(stackImageUpdateService.isCbVersionOk(any(StackDto.class))).thenReturn(false);
        checkImageAction.setFailureEvent(StackImageUpdateEvent.STACK_IMAGE_UPDATE_FAILED_EVENT);

        checkImageAction.execute(stateContext);

        verify(flowMessageService, times(1)).fireEventAndLog(anyLong(), eq(Status.UPDATE_IN_PROGRESS.name()),
                eq(ResourceEvent.STACK_IMAGE_UPDATE_STARTED));
        verify(stackImageUpdateService, times(0)).getNewImageIfVersionsMatch(any(Stack.class), anyString(), eq(null), eq(null));
        verify(eventBus, times(0)).notify(eq(StackImageUpdateEvent.CHECK_IMAGE_VERESIONS_FINISHED_EVENT.event()), any(Event.class));
        verify(eventBus, times(1)).notify(eq(StackImageUpdateEvent.STACK_IMAGE_UPDATE_FAILED_EVENT.event()), any(Event.class));
    }

    @Test
    void checkPackageVersions() {
        ImageUpdateEvent payload = new ImageUpdateEvent(StackImageUpdateEvent.STACK_IMAGE_UPDATE_EVENT.event(), 1L, statedImage);
        when(stateContext.getMessageHeader(HEADERS.DATA.name())).thenReturn(payload);
        when(state.getId()).thenReturn(StackImageUpdateState.CHECK_IMAGE_VERSIONS_STATE);
        when(stackImageUpdateService.checkPackageVersions(any(StackDto.class), any(StatedImage.class))).thenReturn(CheckResult.ok());

        checkPackageVersionsAction.execute(stateContext);

        verify(stackImageUpdateService, times(1)).checkPackageVersions(any(StackDto.class), any(StatedImage.class));
        verify(eventBus, times(1)).notify(eq(StackImageUpdateEvent.CHECK_PACKAGE_VERSIONS_FINISHED_EVENT.event()), any(Event.class));
    }

    @Test
    void checkPackageVersionsNotOk() {
        ImageUpdateEvent payload = new ImageUpdateEvent(StackImageUpdateEvent.CHECK_IMAGE_VERESIONS_FINISHED_EVENT.event(), 1L, statedImage);
        when(stateContext.getMessageHeader(HEADERS.DATA.name())).thenReturn(payload);
        when(state.getId()).thenReturn(StackImageUpdateState.CHECK_PACKAGE_VERSIONS_STATE);
        when(stackImageUpdateService.checkPackageVersions(any(StackDto.class), any(StatedImage.class))).thenReturn(CheckResult.failed(""));
        checkPackageVersionsAction.setFailureEvent(StackImageUpdateEvent.STACK_IMAGE_UPDATE_FAILED_EVENT);

        checkPackageVersionsAction.execute(stateContext);

        verify(stackImageUpdateService, times(1)).checkPackageVersions(any(StackDto.class), any(StatedImage.class));
        verify(eventBus, times(1)).notify(eq(StackImageUpdateEvent.CHECK_PACKAGE_VERSIONS_FINISHED_EVENT.event()), any(Event.class));
        verify(eventBus, times(0)).notify(eq(StackImageUpdateEvent.STACK_IMAGE_UPDATE_FAILED_EVENT.event()), any(Event.class));
    }

    @Test
    void updateImage() {
        ImageUpdateEvent payload = new ImageUpdateEvent(StackImageUpdateEvent.CHECK_PACKAGE_VERSIONS_FINISHED_EVENT.event(), 1L, statedImage);
        when(stateContext.getMessageHeader(HEADERS.DATA.name())).thenReturn(payload);
        when(state.getId()).thenReturn(StackImageUpdateState.UPDATE_IMAGE_STATE);

        updateImageAction.execute(stateContext);

        verify(stackImageService, times(1)).storeNewImageComponent(any(StackDto.class), any(StatedImage.class));
        verify(eventBus, times(1)).notify(eq(StackImageUpdateEvent.UPDATE_IMAGE_FINESHED_EVENT.event()), any(Event.class));
        assertTrue(variables.containsKey(AbstractStackImageUpdateAction.ORIGINAL_IMAGE));
    }

    @Test
    void prepareImageAction() throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        StackEvent payload = new StackEvent(StackImageUpdateEvent.UPDATE_IMAGE_FINESHED_EVENT.event(), 1L);
        when(stateContext.getMessageHeader(HEADERS.DATA.name())).thenReturn(payload);
        when(state.getId()).thenReturn(StackImageUpdateState.IMAGE_PREPARE_STATE);
        when(imageFallbackService.getFallbackImageName(any(), any())).thenReturn("fallbackImage");

        prepareImageAction.execute(stateContext);

        verify(stackCreationService, times(1)).prepareImage(anyLong(), eq(variables));
        verify(eventBus, times(1)).notify(eq(CloudPlatformRequest.selector(PrepareImageRequest.class)), any(Event.class));
    }

    @Test
    void prepareImageActionFallbackThrowsImageNotFoundException() throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        StackEvent payload = new StackEvent(StackImageUpdateEvent.UPDATE_IMAGE_FINESHED_EVENT.event(), 1L);
        when(stateContext.getMessageHeader(HEADERS.DATA.name())).thenReturn(payload);
        when(state.getId()).thenReturn(StackImageUpdateState.IMAGE_PREPARE_STATE);
        when(imageFallbackService.getFallbackImageName(any(), any())).thenThrow(new CloudbreakImageNotFoundException("image not found"));

        prepareImageAction.execute(stateContext);

        verify(stackCreationService, times(1)).prepareImage(anyLong(), eq(variables));
        verify(eventBus, times(1)).notify(eq(CloudPlatformRequest.selector(PrepareImageRequest.class)), any(Event.class));
    }

    @Test
    void prepareImageActionFallbackThrowsImageCatalogException() throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        StackEvent payload = new StackEvent(StackImageUpdateEvent.UPDATE_IMAGE_FINESHED_EVENT.event(), 1L);
        when(stateContext.getMessageHeader(HEADERS.DATA.name())).thenReturn(payload);
        when(state.getId()).thenReturn(StackImageUpdateState.IMAGE_PREPARE_STATE);
        when(imageFallbackService.getFallbackImageName(any(), any())).thenThrow(new CloudbreakImageCatalogException("image catalog not found"));
        prepareImageAction.setFailureEvent(StackImageUpdateEvent.IMAGE_PREPARATION_FAILED_EVENT);

        prepareImageAction.execute(stateContext);

        verify(eventBus, times(1)).notify(eq(StackImageUpdateEvent.IMAGE_PREPARATION_FAILED_EVENT.event()), any(Event.class));
        verify(stackCreationService, times(1)).prepareImage(anyLong(), eq(variables));
        verify(eventBus, never()).notify(eq(CloudPlatformRequest.selector(PrepareImageRequest.class)), any(Event.class));
    }

    @Test
    void setImageAction() {
        Stack stack = new Stack();
        StackEvent payload = new StackEvent(StackImageUpdateEvent.UPDATE_IMAGE_FINESHED_EVENT.event(), 1L);
        when(stateContext.getMessageHeader(HEADERS.DATA.name())).thenReturn(payload);
        when(state.getId()).thenReturn(StackImageUpdateState.SET_IMAGE_STATE);
        when(resourceService.getAllByStackId(anyLong()))
                .thenReturn(Collections.singletonList(new Resource(ResourceType.CLOUDFORMATION_STACK, "cf", stack, "az1")));
        when(resourceToCloudResourceConverter.convert(any(Resource.class)))
                .thenReturn(CloudResource.builder().withType(ResourceType.CLOUDFORMATION_STACK).withName("cfresource").build());

        setImageAction.execute(stateContext);

        verify(eventBus, times(1)).notify(eq(CloudPlatformRequest.selector(UpdateImageRequest.class)), any(Event.class));
    }

    @Test
    void finishAction() {
        CloudPlatformResult payload = new CloudPlatformResult(1L);
        when(stateContext.getMessageHeader(HEADERS.DATA.name())).thenReturn(payload);
        when(state.getId()).thenReturn(StackImageUpdateState.STACK_IMAGE_UPDATE_FINISHED);

        finishAction.execute(stateContext);

        verify(flowMessageService, times(1)).fireEventAndLog(anyLong(), eq(Status.AVAILABLE.name()), eq(ResourceEvent.STACK_IMAGE_UPDATE_FINISHED));
    }

    @Test
    void handleImageUpdateFailure() {
        StackFailureEvent payload =
                new StackFailureEvent(StackImageUpdateEvent.STACK_IMAGE_UPDATE_FAILED_EVENT.event(), 1L, new CloudbreakServiceException("test"));
        when(stateContext.getMessageHeader(HEADERS.DATA.name())).thenReturn(payload);
        when(state.getId()).thenReturn(StackImageUpdateState.STACK_IMAGE_UPDATE_FAILED_STATE);
        when(runningFlows.get(anyString())).thenReturn(flow);

        handleImageUpdateFailureAction.execute(stateContext);

        verify(flowMessageService, times(1)).fireEventAndLog(anyLong(), eq(Status.UPDATE_FAILED.name()),
                eq(ResourceEvent.STACK_IMAGE_UPDATE_FAILED), eq("test"));
        verify(eventBus, times(1)).notify(eq(StackImageUpdateEvent.STACK_IMAGE_UPDATE_FAILE_HANDLED_EVENT.event()), any(Event.class));
        verify(stackUpdater).updateStackStatus(eq(1L), eq(DetailedStackStatus.STACK_IMAGE_UPDATE_FAILED), eq("test"));
        verifyNoInteractions(componentConfigProviderService);
    }
}