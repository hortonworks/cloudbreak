package com.sequenceiq.cloudbreak.shell.commands.common;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sequenceiq.cloudbreak.api.endpoint.ClusterEndpoint;
import com.sequenceiq.cloudbreak.client.CloudbreakClient;
import com.sequenceiq.cloudbreak.shell.completion.HostGroup;
import com.sequenceiq.cloudbreak.shell.model.ShellContext;
import com.sequenceiq.cloudbreak.shell.util.CloudbreakShellUtil;

public class ClusterCommandsTest {

    @InjectMocks
    private ClusterCommands underTest;

    @Mock
    private ShellContext shellContext;

    @Mock
    private CloudbreakClient cloudbreakClient;

    @Mock
    private CloudbreakShellUtil cloudbreakShellUtil;

    @Mock
    private ClusterEndpoint clusterEndpoint;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        underTest = new ClusterCommands(shellContext, cloudbreakShellUtil);

        given(shellContext.cloudbreakClient()).willReturn(cloudbreakClient);
        given(cloudbreakClient.clusterEndpoint()).willReturn(clusterEndpoint);
    }

    @Test
    public void clusterUpscaleForDefaultModeStack() throws Exception {
        Long stackId = 1L;
        String stackIdStr = stackId.toString();
        given(shellContext.isMarathonMode()).willReturn(false);
        given(shellContext.getStackId()).willReturn(stackIdStr);
        given(shellContext.getSelectedMarathonStackId()).willReturn(null);

        HostGroup hostGroup = new HostGroup("master");
        String addNodeResult = underTest.addNode(hostGroup, +1);

        verify(clusterEndpoint).put(eq(stackId), anyObject());
        Assert.assertThat(addNodeResult, containsString("id: " + stackIdStr));
    }

    @Test
    public void clusterUpscaleForMarathonModeStack() throws Exception {
        Long stackId = 42L;
        given(shellContext.isMarathonMode()).willReturn(true);
        given(shellContext.getStackId()).willReturn(null);
        given(shellContext.getSelectedMarathonStackId()).willReturn(stackId);

        HostGroup hostGroup = new HostGroup("master");
        String addNodeResult = underTest.addNode(hostGroup, +1);

        verify(clusterEndpoint).put(eq(stackId), anyObject());
        Assert.assertThat(addNodeResult, containsString("id: " + stackId));
    }

    @Test
    public void clusterDownscaleForDefaultModeStack() throws Exception {
        Long stackId = 1L;
        String stackIdStr = stackId.toString();
        given(shellContext.isMarathonMode()).willReturn(false);
        given(shellContext.getStackId()).willReturn(stackIdStr);
        given(shellContext.getSelectedMarathonStackId()).willReturn(null);

        HostGroup hostGroup = new HostGroup("master");
        String removeNodeResult = underTest.removeNode(hostGroup, -1, false);

        verify(clusterEndpoint).put(eq(stackId), anyObject());
        Assert.assertThat(removeNodeResult, containsString("id: " + stackIdStr));
    }

    @Test
    public void clusterDownscaleForMarathonModeStack() throws Exception {
        Long stackId = 42L;
        given(shellContext.isMarathonMode()).willReturn(true);
        given(shellContext.getStackId()).willReturn(null);
        given(shellContext.getSelectedMarathonStackId()).willReturn(stackId);

        HostGroup hostGroup = new HostGroup("master");
        String removeNodeResult = underTest.removeNode(hostGroup, -1, false);

        verify(clusterEndpoint).put(eq(stackId), anyObject());
        Assert.assertThat(removeNodeResult, containsString("id: " + stackId));
    }

}
