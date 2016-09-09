package com.sequenceiq.cloudbreak.orchestrator.swarm;

import static com.sequenceiq.cloudbreak.orchestrator.swarm.DockerClientUtil.createContainer;
import static com.sequenceiq.cloudbreak.orchestrator.swarm.DockerClientUtil.startContainer;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.NotFoundException;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.InspectContainerCmd;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.command.RemoveContainerCmd;
import com.github.dockerjava.api.command.StartContainerCmd;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;

@RunWith(MockitoJUnitRunner.class)
public class DockerClientUtilTest {

    @Mock
    private DockerClient client;

    @Mock
    private InspectContainerResponse inspectContainerResponse;

    @Mock
    private CreateContainerResponse createContainerResponse;

    @Mock
    private InspectContainerCmd inspectContainerCmd;

    @Mock
    private RemoveContainerCmd removeContainerCmd;

    @Mock
    private CreateContainerCmd createContainerCmd;

    @Mock
    private StartContainerCmd startContainerCmd;

    @Mock
    private InspectContainerResponse.ContainerState containerState;

    @Before
    public void before() {
        reset(client);
        reset(createContainerCmd);
        reset(inspectContainerResponse);
        reset(removeContainerCmd);
        reset(inspectContainerCmd);
        reset(containerState);
        reset(startContainerCmd);
        reset(createContainerResponse);
    }

    @Test
    public void removeContainerIExistWhenInspectReturnWithNull() {
        when(client.inspectContainerCmd(anyString())).thenReturn(inspectContainerCmd);
        when(inspectContainerCmd.exec()).thenReturn(null);
        when(createContainerCmd.getName()).thenReturn("ambari-server");

        createContainer(client, createContainerCmd, "vm1");

        verify(client, times(1)).inspectContainerCmd(anyString());
        verify(client, times(0)).removeContainerCmd(anyString());
    }

    @Test
    public void removeContainerIExistWhenInspectDropNotFoundException() {
        when(client.inspectContainerCmd(anyString())).thenThrow(new NotFoundException("notfound"));
        when(createContainerCmd.getName()).thenReturn("ambari-server");

        createContainer(client, createContainerCmd, "vm1");

        verify(client, times(1)).inspectContainerCmd(anyString());
        verify(client, times(0)).removeContainerCmd(anyString());
    }

    @Test(expected = IllegalArgumentException.class)
    public void removeContainerIExistWhenInspectDropActualException() {
        when(client.inspectContainerCmd(anyString())).thenThrow(new IllegalArgumentException("illegal argument"));
        when(createContainerCmd.getName()).thenReturn("ambari-server");

        createContainer(client, createContainerCmd, "vm1");
    }

    @Test
    public void removeContainerIExistWhenInspectReturnWithValueThenRemoveContainer() {
        when(client.inspectContainerCmd(anyString())).thenReturn(inspectContainerCmd);
        when(inspectContainerCmd.exec()).thenReturn(inspectContainerResponse);
        when(inspectContainerResponse.getId()).thenReturn("xx666xx");
        InspectContainerResponse.ContainerState state = mock(InspectContainerResponse.ContainerState.class);
        when(inspectContainerResponse.getState()).thenReturn(state);
        when(state.isRunning()).thenReturn(false);
        when(client.removeContainerCmd(anyString())).thenReturn(removeContainerCmd);
        when(removeContainerCmd.withForce(anyBoolean())).thenReturn(removeContainerCmd);
        when(removeContainerCmd.exec()).thenReturn(null);
        when(createContainerCmd.getName()).thenReturn("ambari-server");

        createContainer(client, createContainerCmd, "vm1");

        verify(client, times(1)).inspectContainerCmd(anyString());
        verify(client, times(1)).removeContainerCmd(anyString());
    }

    @Test
    public void startContainerWhenEverythingWorksFine() throws Exception {
        when(inspectContainerResponse.getState()).thenReturn(containerState);
        when(containerState.isRunning()).thenReturn(true);
        when(inspectContainerCmd.exec()).thenReturn(inspectContainerResponse);
        when(client.inspectContainerCmd(anyString())).thenReturn(inspectContainerCmd);
        when(client.startContainerCmd(anyString())).thenReturn(startContainerCmd);
        startContainer(client, "OK");

        verify(startContainerCmd, times(1)).exec();
    }

    @Test(expected = CloudbreakOrchestratorFailedException.class)
    public void startContainerButContainerNotStarted() throws Exception {
        when(inspectContainerResponse.getState()).thenReturn(containerState);
        when(containerState.isRunning()).thenReturn(false);
        when(inspectContainerCmd.exec()).thenReturn(inspectContainerResponse);
        when(client.inspectContainerCmd(anyString())).thenReturn(inspectContainerCmd);
        when(client.startContainerCmd(anyString())).thenReturn(startContainerCmd);

        startContainer(client, "OK");
    }

    @Test
    public void createContainerWhenEverythingWorksFine() {
        when(createContainerResponse.getId()).thenReturn("xxx666xxx");
        when(client.inspectContainerCmd(anyString())).thenReturn(inspectContainerCmd);
        when(inspectContainerCmd.exec()).thenReturn(inspectContainerResponse);
        when(createContainerCmd.exec()).thenReturn(createContainerResponse);

        createContainer(client, createContainerCmd, "vm12");

        //No exception
    }


}