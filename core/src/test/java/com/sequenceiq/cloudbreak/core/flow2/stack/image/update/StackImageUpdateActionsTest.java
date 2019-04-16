package com.sequenceiq.cloudbreak.core.flow2.stack.image.update;

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.statemachine.ExtendedState;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.state.State;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformRequest;
import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult;
import com.sequenceiq.cloudbreak.cloud.event.resource.UpdateImageRequest;
import com.sequenceiq.cloudbreak.cloud.event.setup.PrepareImageRequest;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.reactor.ErrorHandlerAwareReactorEventFactory;
import com.sequenceiq.cloudbreak.common.type.ResourceType;
import com.sequenceiq.cloudbreak.converter.spi.CredentialToCloudCredentialConverter;
import com.sequenceiq.cloudbreak.converter.spi.ResourceToCloudResourceConverter;
import com.sequenceiq.cloudbreak.converter.spi.StackToCloudStackConverter;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.core.flow2.CheckResult;
import com.sequenceiq.cloudbreak.core.flow2.Flow;
import com.sequenceiq.cloudbreak.core.flow2.FlowRegister;
import com.sequenceiq.cloudbreak.core.flow2.MessageFactory.HEADERS;
import com.sequenceiq.cloudbreak.core.flow2.event.StackImageUpdateTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.stack.AbstractStackFailureAction;
import com.sequenceiq.cloudbreak.core.flow2.stack.CloudbreakFlowMessageService;
import com.sequenceiq.cloudbreak.core.flow2.stack.StackFailureContext;
import com.sequenceiq.cloudbreak.core.flow2.stack.provision.action.StackCreationService;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.view.StackView;
import com.sequenceiq.cloudbreak.domain.workspace.User;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;
import com.sequenceiq.cloudbreak.message.Msg;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.ImageUpdateEvent;
import com.sequenceiq.cloudbreak.service.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.service.ComponentConfigProviderService;
import com.sequenceiq.cloudbreak.service.StackUpdater;
import com.sequenceiq.cloudbreak.service.image.ImageService;
import com.sequenceiq.cloudbreak.service.image.StatedImage;
import com.sequenceiq.cloudbreak.service.resource.ResourceService;
import com.sequenceiq.cloudbreak.service.stack.StackService;

import reactor.bus.Event;
import reactor.bus.EventBus;

public class StackImageUpdateActionsTest {

    private static final String USER_ID = "horton@hortonworks.com";

    private static final Long WORKSPACE_ID = 1L;

    @Mock
    private CloudbreakFlowMessageService flowMessageService;

    @Mock
    private StackService stackService;

    @Mock
    private StackCreationService stackCreationService;

    @Mock
    private CredentialToCloudCredentialConverter credentialConverter;

    @Mock
    private StackToCloudStackConverter cloudStackConverter;

    @Mock
    private StackImageUpdateService stackImageUpdateService;

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

    @Before
    public void setUp() throws CloudbreakImageNotFoundException {
        MockitoAnnotations.initMocks(this);
        when(stateContext.getMessageHeader(HEADERS.FLOW_ID.name())).thenReturn("flowId");
        when(stateContext.getExtendedState()).thenReturn(extendedState);
        when(stateContext.getStateMachine()).thenReturn(stateMachine);
        when(stateMachine.getState()).thenReturn(state);
        when(extendedState.getVariables()).thenReturn(variables);
        when(runningFlows.getFlowChainId(anyString())).thenReturn("flowchainid");
        when(reactorEventFactory.createEvent(any(Map.class), any(Object.class))).thenReturn(new Event("dummy"));
        when(imageService.getImage(anyLong())).thenReturn(image);

        User user = new User();
        user.setUserId("horton@hortonworks.com");
        user.setUserCrn("testCrn");
        user.setUserName("Alma ur");
        Workspace workspace = new Workspace();
        workspace.setId(1L);
        Stack stack = new Stack();
        stack.setCreator(user);
        stack.setWorkspace(workspace);
        stack.setId(1L);
        stack.setRegion("region");
        stack.setAvailabilityZone("az");
        when(stackService.getByIdWithListsInTransaction(anyLong())).thenReturn(stack);
        when(stackService.getById(anyLong())).thenReturn(stack);

        variables.clear();
    }

    @Test
    public void checkImageVersion() {
        StackImageUpdateTriggerEvent payload = new StackImageUpdateTriggerEvent(StackImageUpdateEvent.STACK_IMAGE_UPDATE_EVENT.event(), 1L, "imageId");
        when(stateContext.getMessageHeader(HEADERS.DATA.name())).thenReturn(payload);
        when(state.getId()).thenReturn(StackImageUpdateState.CHECK_IMAGE_VERSIONS_STATE);
        when(stackImageUpdateService.isCbVersionOk(any(Stack.class))).thenReturn(true);

        checkImageAction.execute(stateContext);

        verify(flowMessageService, times(1)).fireEventAndLog(anyLong(), eq(Msg.STACK_IMAGE_UPDATE_STARTED),
                eq(Status.UPDATE_IN_PROGRESS.name()));
        verify(stackImageUpdateService, times(1)).getNewImageIfVersionsMatch(any(Stack.class), anyString(), eq(null), eq(null));
        verify(eventBus, times(1)).notify(eq(StackImageUpdateEvent.CHECK_IMAGE_VERESIONS_FINISHED_EVENT.event()), any(Event.class));
    }

    @Test
    public void checkImageVersionNotOk() {
        StackImageUpdateTriggerEvent payload = new StackImageUpdateTriggerEvent(StackImageUpdateEvent.STACK_IMAGE_UPDATE_EVENT.event(), 1L, "imageId");
        when(stateContext.getMessageHeader(HEADERS.DATA.name())).thenReturn(payload);
        when(state.getId()).thenReturn(StackImageUpdateState.CHECK_IMAGE_VERSIONS_STATE);
        when(stackImageUpdateService.isCbVersionOk(any(Stack.class))).thenReturn(false);
        checkImageAction.setFailureEvent(StackImageUpdateEvent.STACK_IMAGE_UPDATE_FAILED_EVENT);

        checkImageAction.execute(stateContext);

        verify(flowMessageService, times(1)).fireEventAndLog(anyLong(), eq(Msg.STACK_IMAGE_UPDATE_STARTED),
                eq(Status.UPDATE_IN_PROGRESS.name()));
        verify(stackImageUpdateService, times(0)).getNewImageIfVersionsMatch(any(Stack.class), anyString(), eq(null), eq(null));
        verify(eventBus, times(0)).notify(eq(StackImageUpdateEvent.CHECK_IMAGE_VERESIONS_FINISHED_EVENT.event()), any(Event.class));
        verify(eventBus, times(1)).notify(eq(StackImageUpdateEvent.STACK_IMAGE_UPDATE_FAILED_EVENT.event()), any(Event.class));
    }

    @Test
    public void checkPackageVersions() {
        ImageUpdateEvent payload = new ImageUpdateEvent(StackImageUpdateEvent.STACK_IMAGE_UPDATE_EVENT.event(), 1L, statedImage);
        when(stateContext.getMessageHeader(HEADERS.DATA.name())).thenReturn(payload);
        when(state.getId()).thenReturn(StackImageUpdateState.CHECK_IMAGE_VERSIONS_STATE);
        when(stackImageUpdateService.checkPackageVersions(any(Stack.class), any(StatedImage.class))).thenReturn(CheckResult.ok());

        checkPackageVersionsAction.execute(stateContext);

        verify(stackImageUpdateService, times(1)).checkPackageVersions(any(Stack.class), any(StatedImage.class));
        verify(eventBus, times(1)).notify(eq(StackImageUpdateEvent.CHECK_PACKAGE_VERSIONS_FINISHED_EVENT.event()), any(Event.class));
    }

    @Test
    public void checkPackageVersionsNotOk() {
        ImageUpdateEvent payload = new ImageUpdateEvent(StackImageUpdateEvent.CHECK_IMAGE_VERESIONS_FINISHED_EVENT.event(), 1L, statedImage);
        when(stateContext.getMessageHeader(HEADERS.DATA.name())).thenReturn(payload);
        when(state.getId()).thenReturn(StackImageUpdateState.CHECK_PACKAGE_VERSIONS_STATE);
        when(stackImageUpdateService.checkPackageVersions(any(Stack.class), any(StatedImage.class))).thenReturn(CheckResult.failed(""));
        checkPackageVersionsAction.setFailureEvent(StackImageUpdateEvent.STACK_IMAGE_UPDATE_FAILED_EVENT);

        checkPackageVersionsAction.execute(stateContext);

        verify(stackImageUpdateService, times(1)).checkPackageVersions(any(Stack.class), any(StatedImage.class));
        verify(eventBus, times(0)).notify(eq(StackImageUpdateEvent.CHECK_PACKAGE_VERSIONS_FINISHED_EVENT.event()), any(Event.class));
        verify(eventBus, times(1)).notify(eq(StackImageUpdateEvent.STACK_IMAGE_UPDATE_FAILED_EVENT.event()), any(Event.class));
    }

    @Test
    public void updateImage() {
        ImageUpdateEvent payload = new ImageUpdateEvent(StackImageUpdateEvent.CHECK_PACKAGE_VERSIONS_FINISHED_EVENT.event(), 1L, statedImage);
        when(stateContext.getMessageHeader(HEADERS.DATA.name())).thenReturn(payload);
        when(state.getId()).thenReturn(StackImageUpdateState.UPDATE_IMAGE_STATE);

        updateImageAction.execute(stateContext);

        verify(stackImageUpdateService, times(1)).storeNewImageComponent(any(Stack.class), any(StatedImage.class));
        verify(eventBus, times(1)).notify(eq(StackImageUpdateEvent.UPDATE_IMAGE_FINESHED_EVENT.event()), any(Event.class));
        assertTrue(variables.containsKey(AbstractStackImageUpdateAction.ORIGINAL_IMAGE));
    }

    @Test
    public void prepareImageAction() {
        StackEvent payload = new StackEvent(StackImageUpdateEvent.UPDATE_IMAGE_FINESHED_EVENT.event(), 1L);
        when(stateContext.getMessageHeader(HEADERS.DATA.name())).thenReturn(payload);
        when(state.getId()).thenReturn(StackImageUpdateState.IMAGE_PREPARE_STATE);

        prepareImageAction.execute(stateContext);

        verify(stackCreationService, times(1)).prepareImage(any(Stack.class));
        verify(eventBus, times(1)).notify(eq(CloudPlatformRequest.selector(PrepareImageRequest.class)), any(Event.class));
    }

    @Test
    public void setImageAction() {
        Stack stack = new Stack();
        StackEvent payload = new StackEvent(StackImageUpdateEvent.UPDATE_IMAGE_FINESHED_EVENT.event(), 1L);
        when(stateContext.getMessageHeader(HEADERS.DATA.name())).thenReturn(payload);
        when(state.getId()).thenReturn(StackImageUpdateState.SET_IMAGE_STATE);
        when(resourceService.findAllByStackId(anyLong()))
                .thenReturn(Collections.singletonList(new Resource(ResourceType.CLOUDFORMATION_STACK, "cf", stack)));
        when(resourceToCloudResourceConverter.convert(any(Resource.class)))
                .thenReturn(CloudResource.builder().type(ResourceType.CLOUDFORMATION_STACK).name("cfresource").build());

        setImageAction.execute(stateContext);

        verify(eventBus, times(1)).notify(eq(CloudPlatformRequest.selector(UpdateImageRequest.class)), any(Event.class));
    }

    @Test
    public void finishAction() {
        CloudPlatformResult payload = new CloudPlatformResult(new CloudPlatformRequest(
                new CloudContext(1L, "asdf", "Asdf", USER_ID, WORKSPACE_ID), null));
        when(stateContext.getMessageHeader(HEADERS.DATA.name())).thenReturn(payload);
        when(state.getId()).thenReturn(StackImageUpdateState.STACK_IMAGE_UPDATE_FINISHED);

        finishAction.execute(stateContext);

        verify(flowMessageService, times(1)).fireEventAndLog(anyLong(), eq(Msg.STACK_IMAGE_UPDATE_FINISHED),
                eq(Status.AVAILABLE.name()));
    }

    @Test
    public void handleImageUpdateFailure() {
        StackFailureEvent payload =
                new StackFailureEvent(StackImageUpdateEvent.STACK_IMAGE_UPDATE_FAILED_EVENT.event(), 1L, new CloudbreakServiceException("test"));
        when(stateContext.getMessageHeader(HEADERS.DATA.name())).thenReturn(payload);
        when(state.getId()).thenReturn(StackImageUpdateState.STACK_IMAGE_UPDATE_FAILED_STATE);
        when(stackService.getViewByIdWithoutAuth(anyLong())).thenReturn(new StackView(1L, null, null, null));
        when(runningFlows.get(anyString())).thenReturn(flow);

        handleImageUpdateFailureAction.execute(stateContext);

        verify(flowMessageService, times(1)).fireEventAndLog(anyLong(), eq(Msg.STACK_IMAGE_UPDATE_FAILED),
                eq(Status.UPDATE_FAILED.name()), eq("test"));
        verify(eventBus, times(1)).notify(eq(StackImageUpdateEvent.STACK_IMAGE_UPDATE_FAILE_HANDLED_EVENT.event()), any(Event.class));

    }
}