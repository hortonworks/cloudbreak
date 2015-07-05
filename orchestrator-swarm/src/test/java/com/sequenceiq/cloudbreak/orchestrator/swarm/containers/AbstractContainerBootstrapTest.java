package com.sequenceiq.cloudbreak.orchestrator.swarm.containers;

import static org.junit.Assert.assertTrue;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.StartContainerCmd;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.RestartPolicy;
import com.sequenceiq.cloudbreak.orchestrator.containers.ContainerBootstrap;
import com.sequenceiq.cloudbreak.orchestrator.swarm.DockerClientUtil;

import jersey.repackaged.com.google.common.collect.Sets;

public abstract class AbstractContainerBootstrapTest {
    protected static final String DUMMY_CLOUD_PLATFORM = "GCP";
    protected static final String DUMMY_GENERATED_ID = "dummyGeneratedId";
    protected static final String DUMMY_IMAGE = "sequenceiq/dummy:0.0.1";
    protected static final String DUMMY_NODE = "dummyNode";
    protected static final String DUMMY_GETAWAY_ADDRESS = "25.26.27.1";
    protected static final Set<String> DUMMY_VOLUMES = Sets.newHashSet("/var/path1", "/var/path2");
    private static final String DUMMY_CONTAINER_ID = "dummyContainerId";

    private ContainerBootstrap underTest;

    @Mock
    private DockerClient mockedDockerClient;

    @Mock
    private DockerClientUtil mockedDockerClientUtil;

    @Mock
    private CreateContainerCmd mockedCreateContainerCmd;

    @Mock
    private StartContainerCmd mockedStartContainerCmd;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        underTest = getTestInstance();
    }

    @Test
    public void testCall() throws Exception {
        // GIVEN
        mockAll();
        // WHEN
        boolean result = underTest.call();
        // THEN
        verify(getMockedDockerClientUtil(), times(1)).startContainer(getMockedDockerClient(), getMockedStartContainerCmd());
        assertTrue(result);
    }

    @Test(expected = RuntimeException.class)
    public void testCallWhenCreateContainerThrowsException() throws Exception {
        // GIVEN
        mockDockerClient();
        mockCreateContainerCommand();
        given(getMockedDockerClientUtil().createContainer(getMockedDockerClient(), getMockedCreateContainerCmd()))
                .willThrow(new RuntimeException());
        // WHEN
        underTest.call();
        // THEN
        verify(getMockedDockerClientUtil(), times(1)).createContainer(getMockedDockerClient(), getMockedCreateContainerCmd());
        verify(getMockedDockerClientUtil(), times(0)).startContainer(getMockedDockerClient(), getMockedStartContainerCmd());
    }

    @Test(expected = RuntimeException.class)
    public void testCallWhenStartContainerThrowsException() throws Exception {
        // GIVEN
        mockDockerClient();
        mockCreateContainerCommand();
        doThrow(new RuntimeException()).when(getMockedDockerClientUtil())
                .startContainer(getMockedDockerClient(), getMockedStartContainerCmd());
        given(getMockedDockerClientUtil().createContainer(getMockedDockerClient(), getMockedCreateContainerCmd()))
                .willThrow(new RuntimeException());
        // WHEN
        underTest.call();
        // THEN
        verify(getMockedDockerClientUtil(), times(1)).startContainer(getMockedDockerClient(), getMockedStartContainerCmd());
    }

    public void mockAll() throws Exception {
        mockDockerClient();
        mockDockerClientUtil();
        mockCreateContainerCommand();
        mockStartContainerCommand();
    }

    public void mockDockerClient() {
        given(mockedDockerClient.createContainerCmd(anyString())).willReturn(mockedCreateContainerCmd);
        given(mockedDockerClient.startContainerCmd(anyString())).willReturn(mockedStartContainerCmd);
    }

    public void mockDockerClientUtil() throws Exception {
        given(mockedDockerClientUtil.createContainer(mockedDockerClient, mockedCreateContainerCmd)).willReturn(DUMMY_CONTAINER_ID);
        doNothing().when(mockedDockerClientUtil).startContainer(mockedDockerClient, mockedStartContainerCmd);
    }

    public void mockCreateContainerCommand() {
        given(mockedCreateContainerCmd.withCmd(anyString())).willReturn(mockedCreateContainerCmd);
        given(mockedCreateContainerCmd.withName(anyString())).willReturn(mockedCreateContainerCmd);
        given(mockedCreateContainerCmd.withHostConfig(any(HostConfig.class))).willReturn(mockedCreateContainerCmd);
        given(mockedCreateContainerCmd.withEnv(Matchers.<String>anyVararg())).willReturn(mockedCreateContainerCmd);
        given(mockedCreateContainerCmd.withExposedPorts(Matchers.<ExposedPort>anyVararg())).willReturn(mockedCreateContainerCmd);
    }

    public void mockStartContainerCommand() {
        given(mockedStartContainerCmd.withNetworkMode(anyString())).willReturn(mockedStartContainerCmd);
        given(mockedStartContainerCmd.withPortBindings(Matchers.<PortBinding>anyVararg())).willReturn(mockedStartContainerCmd);
        given(mockedStartContainerCmd.withRestartPolicy(RestartPolicy.alwaysRestart())).willReturn(mockedStartContainerCmd);
        given(mockedStartContainerCmd.withBinds(Matchers.<Bind>anyVararg())).willReturn(mockedStartContainerCmd);
        given(mockedStartContainerCmd.withPrivileged(true)).willReturn(mockedStartContainerCmd);
    }

    public DockerClient getMockedDockerClient() {
        return mockedDockerClient;
    }

    public DockerClientUtil getMockedDockerClientUtil() {
        return mockedDockerClientUtil;
    }

    public CreateContainerCmd getMockedCreateContainerCmd() {
        return mockedCreateContainerCmd;
    }

    public StartContainerCmd getMockedStartContainerCmd() {
        return mockedStartContainerCmd;
    }

    public abstract ContainerBootstrap getTestInstance();
}
