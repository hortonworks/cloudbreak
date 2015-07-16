package com.sequenceiq.cloudbreak.orchestrator.swarm.containers;

import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Set;

import javax.ws.rs.ProcessingException;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.InspectContainerCmd;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.command.StartContainerCmd;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.HostConfig;
import com.sequenceiq.cloudbreak.orchestrator.containers.ContainerBootstrap;
import com.sequenceiq.cloudbreak.orchestrator.model.LogVolumePath;

import jersey.repackaged.com.google.common.collect.Sets;

public abstract class AbstractContainerBootstrapTest {
    protected static final String DUMMY_CLOUD_PLATFORM = "GCP";
    protected static final String DUMMY_GENERATED_ID = "dummyGeneratedId";
    protected static final String DUMMY_IMAGE = "sequenceiq/dummy:0.0.1";
    protected static final String DUMMY_NODE = "dummyNode";
    protected static final String DUMMY_GETAWAY_ADDRESS = "25.26.27.1";
    protected static final Set<String> DUMMY_VOLUMES = Sets.newHashSet("/var/path1", "/var/path2");
    protected static final String[] CMD = new String[]{"cmd1", "cmd2"};
    protected static final LogVolumePath DUMMY_LOG_VOLUME = new LogVolumePath("/var/path1", "/var/path2");
    private static final String DUMMY_CONTAINER_ID = "dummyContainerId";


    private ContainerBootstrap underTest;

    @Mock
    private DockerClient mockedDockerClient;

    @Mock
    private CreateContainerCmd mockedCreateContainerCmd;

    @Mock
    private StartContainerCmd mockedStartContainerCmd;

    @Mock
    private InspectContainerCmd inspectContainerCmd;

    @Mock
    private InspectContainerResponse inspectContainerResponse;

    @Mock
    private CreateContainerResponse createContainerResponse;


    @Mock
    private InspectContainerResponse.ContainerState containerState;


    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        underTest = getTestInstance();
    }

    @Test
    public void testCall() throws Exception {
        // GIVEN
        mockDockerClient();
        // WHEN
        underTest.call();
        // THEN
        verify(getMockedDockerClient(), times(1)).createContainerCmd(anyString());
    }

    @Test(expected = ProcessingException.class)
    public void testCallWhenCreateContainerThrowsException() throws Exception {
        // GIVEN
        mockDockerClient();
        doThrow(new ProcessingException("EX")).when(mockedDockerClient)
                .createContainerCmd(anyString());
        // WHEN
        underTest.call();
    }

    @Test(expected = ProcessingException.class)
    public void testCallWhenStartContainerThrowsException() throws Exception {
        // GIVEN
        mockDockerClient();
        doThrow(new ProcessingException("EX")).when(mockedDockerClient)
                .startContainerCmd(anyString());
        // WHEN
        underTest.call();
    }

    public void mockDockerClient() {
        given(mockedDockerClient.createContainerCmd(anyString())).willReturn(mockedCreateContainerCmd);
        given(mockedDockerClient.startContainerCmd(anyString())).willReturn(mockedStartContainerCmd);
        given(mockedDockerClient.inspectContainerCmd(anyString())).willReturn(inspectContainerCmd);
        mockCreateContainerCommand();
        mockInspectcontainerCmd();

    }


    private void mockCreateContainerCommand() {
        given(mockedCreateContainerCmd.withCmd(anyString())).willReturn(mockedCreateContainerCmd);
        given(mockedCreateContainerCmd.withCmd(Matchers.<String>anyVararg())).willReturn(mockedCreateContainerCmd);
        given(mockedCreateContainerCmd.withName(anyString())).willReturn(mockedCreateContainerCmd);
        given(mockedCreateContainerCmd.withHostConfig(any(HostConfig.class))).willReturn(mockedCreateContainerCmd);
        given(mockedCreateContainerCmd.withEnv(Matchers.<String>anyVararg())).willReturn(mockedCreateContainerCmd);
        given(mockedCreateContainerCmd.withExposedPorts(Matchers.<ExposedPort>anyVararg())).willReturn(mockedCreateContainerCmd);
        given(mockedCreateContainerCmd.exec()).willReturn(createContainerResponse);
    }

    private void mockInspectcontainerCmd() {
        given(inspectContainerCmd.exec()).willReturn(inspectContainerResponse);
        given(inspectContainerResponse.getState()).willReturn(containerState);
        given(containerState.isRunning()).willReturn(true);
    }



    public DockerClient getMockedDockerClient() {
        return mockedDockerClient;
    }


    public abstract ContainerBootstrap getTestInstance();
}
