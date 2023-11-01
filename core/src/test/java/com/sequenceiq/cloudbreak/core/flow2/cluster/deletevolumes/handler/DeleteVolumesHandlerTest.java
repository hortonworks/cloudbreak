package com.sequenceiq.cloudbreak.core.flow2.cluster.deletevolumes.handler;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.deletevolumes.DeleteVolumesEvent.DELETE_VOLUMES_FINISHED_EVENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackDeleteVolumesRequest;
import com.sequenceiq.cloudbreak.cloud.Authenticator;
import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.core.flow2.cluster.deletevolumes.DeleteVolumesService;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.DeleteVolumesHandlerRequest;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.util.StackUtil;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;

@ExtendWith(MockitoExtension.class)
public class DeleteVolumesHandlerTest {

    @Mock
    private StackDtoService stackDtoService;

    @Mock
    private DeleteVolumesService deleteVolumesService;

    @Mock
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Mock
    private StackUtil stackUtil;

    @Mock
    private EventBus eventBus;

    @InjectMocks
    private DeleteVolumesHandler underTest;

    @Mock
    private StackDeleteVolumesRequest stackDeleteVolumesRequest;

    @Mock
    private Authenticator authenticator;

    @Mock
    private CloudConnector cloudConnector;

    @Mock
    private AuthenticatedContext authenticatedContext;

    @Mock
    private CloudCredential cloudCredential;

    @Captor
    private ArgumentCaptor<String> selectorCaptor;

    @Test
    public void testDeleteVolumesFinishedAction() throws Exception {
        doReturn(1L).when(stackDeleteVolumesRequest).getStackId();
        doReturn("TEST").when(stackDeleteVolumesRequest).getGroup();
        CloudResource resource = mock(CloudResource.class);
        DeleteVolumesHandlerRequest deleteRequest = new DeleteVolumesHandlerRequest(List.of(resource), stackDeleteVolumesRequest, "MOCK", Set.of());
        Event event = new Event<>(deleteRequest);
        StackDto stackDto = mock(StackDto.class);
        Workspace workspace = new Workspace();
        workspace.setId(1L);
        doReturn(workspace).when(stackDto).getWorkspace();
        doReturn("crn:cdp:iam:us-west-1:1234:user:__internal__actor__").when(stackDto).getResourceCrn();
        doReturn("crn:cdp:iam:us-west-1:1234:user:__internal__actor__").when(stackDto).getEnvironmentCrn();
        doReturn(stackDto).when(stackDtoService).getById(eq(1L));
        doReturn(cloudConnector).when(cloudPlatformConnectors).get(any());
        doReturn(authenticator).when(cloudConnector).authentication();
        doReturn(authenticatedContext).when(authenticator).authenticate(any(), any());
        doReturn(cloudCredential).when(stackUtil).getCloudCredential(anyString());
        underTest.accept(event);
        verify(eventBus).notify(selectorCaptor.capture(), any());
        verify(deleteVolumesService).updateScriptsAndRebootInstances(eq(1L), eq("TEST"));
        assertEquals(DELETE_VOLUMES_FINISHED_EVENT.event(), selectorCaptor.getValue());
    }

    @Test
    public void testDeleteVolumesFinishedFailureAction() throws Exception {
        doReturn(1L).when(stackDeleteVolumesRequest).getStackId();
        doReturn("TEST").when(stackDeleteVolumesRequest).getGroup();
        CloudResource resource = mock(CloudResource.class);
        DeleteVolumesHandlerRequest deleteRequest = new DeleteVolumesHandlerRequest(List.of(resource), stackDeleteVolumesRequest, "MOCK", Set.of());
        Event event = new Event<>(deleteRequest);
        StackDto stackDto = mock(StackDto.class);
        Workspace workspace = new Workspace();
        workspace.setId(1L);
        doReturn(workspace).when(stackDto).getWorkspace();
        doReturn("crn:cdp:iam:us-west-1:1234:user:__internal__actor__").when(stackDto).getResourceCrn();
        doReturn("crn:cdp:iam:us-west-1:1234:user:__internal__actor__").when(stackDto).getEnvironmentCrn();
        doReturn(stackDto).when(stackDtoService).getById(eq(1L));
        doReturn(cloudConnector).when(cloudPlatformConnectors).get(any());
        doReturn(authenticator).when(cloudConnector).authentication();
        doReturn(authenticatedContext).when(authenticator).authenticate(any(), any());
        doReturn(cloudCredential).when(stackUtil).getCloudCredential(anyString());
        doThrow(new CloudbreakException("TEST")).when(deleteVolumesService).detachResources(any(), any(), any());
        underTest.accept(event);
        verify(eventBus).notify(selectorCaptor.capture(), any());
        verify(deleteVolumesService, times(0)).updateScriptsAndRebootInstances(anyLong(), anyString());
        verify(deleteVolumesService, times(0)).deleteResources(any(), any(), any());
        assertEquals("DELETEVOLUMESFAILEDEVENT_ERROR", selectorCaptor.getValue());
    }
}
