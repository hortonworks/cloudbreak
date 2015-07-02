package com.sequenceiq.cloudbreak.orchestrator.swarm;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
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

    private DockerClientUtil underTest = new DockerClientUtil();

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
    public void removeContainerIExistWhenInspectReturnWithNull() throws Exception {
        when(client.inspectContainerCmd(anyString())).thenReturn(inspectContainerCmd);
        when(inspectContainerCmd.exec()).thenReturn(null);

        underTest.removeContainerIfExist(client, "ambari-server");

        verify(client, times(1)).inspectContainerCmd(anyString());
        verify(client, times(0)).removeContainerCmd(anyString());
    }

    @Test
    public void removeContainerIExistWhenInspectDropNotFoundException() throws Exception {
        when(client.inspectContainerCmd(anyString())).thenThrow(new NotFoundException("notfound"));

        underTest.removeContainerIfExist(client, "ambari-server");

        verify(client, times(1)).inspectContainerCmd(anyString());
        verify(client, times(0)).removeContainerCmd(anyString());
    }

    @Test(expected = IllegalArgumentException.class)
    public void removeContainerIExistWhenInspectDropActualException() throws Exception {
        when(client.inspectContainerCmd(anyString())).thenThrow(new IllegalArgumentException("illegal argument"));

        underTest.removeContainerIfExist(client, "ambari-server");

        verify(client, times(1)).inspectContainerCmd(anyString());
        verify(client, times(0)).removeContainerCmd(anyString());
    }

    @Test
    public void removeContainerIExistWhenInspectReturnWithValueThenRemoveContainer() throws Exception {
        when(client.inspectContainerCmd(anyString())).thenReturn(inspectContainerCmd);
        when(inspectContainerCmd.exec()).thenReturn(inspectContainerResponse);
        when(inspectContainerResponse.getId()).thenReturn("xx666xx");
        when(client.removeContainerCmd(anyString())).thenReturn(removeContainerCmd);
        when(removeContainerCmd.withForce(anyBoolean())).thenReturn(removeContainerCmd);
        when(removeContainerCmd.exec()).thenReturn(null);

        underTest.removeContainerIfExist(client, "ambari-server");

        verify(client, times(1)).inspectContainerCmd(anyString());
        verify(client, times(1)).removeContainerCmd(anyString());
    }

    @Test
    public void inspectContainerWhenEverythingWorksFine() throws Exception {
        when(inspectContainerCmd.exec()).thenReturn(inspectContainerResponse);

        assertEquals(inspectContainerResponse, underTest.inspectContainer(inspectContainerCmd));
    }

    @Test
    public void inspectContainerWhenInspectNotWorksInTheFirstTimeThenRetryAgain() throws Exception {
        when(inspectContainerCmd.exec()).thenReturn(null).thenReturn(inspectContainerResponse);

        assertEquals(inspectContainerResponse, underTest.inspectContainer(inspectContainerCmd));

        verify(inspectContainerCmd, times(2)).exec();
    }

    @Test(expected = IllegalArgumentException.class)
    public void inspectContainerWhenInspectNotWorksAndExceptionComesAndDrops() throws Exception {
        when(inspectContainerCmd.exec()).thenThrow(new IllegalArgumentException("illegalargumentexception"));

        underTest.inspectContainer(inspectContainerCmd);
    }

    @Test
    public void startContainerWhenEverythingWorksFine() throws Exception {
        DockerClientUtil underTestSpy = spy(underTest);
        doReturn(inspectContainerResponse).when(underTestSpy).inspectContainer(any(InspectContainerCmd.class));
        when(inspectContainerResponse.getState()).thenReturn(containerState);
        when(containerState.isRunning()).thenReturn(true);
        when(startContainerCmd.exec()).thenReturn(null);

        underTest.startContainer(client, startContainerCmd);

        verify(startContainerCmd, times(1)).exec();
    }

    @Test
    public void waitForContainerWhenEverythingWorksFine() throws Exception {
        DockerClientUtil underTestSpy = spy(underTest);
        doReturn(inspectContainerResponse).when(underTestSpy).inspectContainer(any(InspectContainerCmd.class));
        when(inspectContainerCmd.exec()).thenReturn(inspectContainerResponse);

        assertEquals(inspectContainerResponse, underTest.waitForContainer(inspectContainerCmd));
    }

    @Test
    public void createContainerWhenEverythingWorksFine() throws Exception {
        DockerClientUtil underTestSpy = spy(underTest);
        doReturn(inspectContainerResponse).when(underTestSpy).inspectContainer(any(InspectContainerCmd.class));
        doNothing().when(underTestSpy).removeContainerIfExist(any(DockerClient.class), anyString());
        when(createContainerResponse.getId()).thenReturn("xxx666xxx");
        when(client.inspectContainerCmd(anyString())).thenReturn(inspectContainerCmd);
        when(inspectContainerCmd.exec()).thenReturn(inspectContainerResponse);
        when(createContainerCmd.exec()).thenReturn(createContainerResponse);

        assertEquals("xxx666xxx", underTest.createContainer(client,  createContainerCmd));
    }


}