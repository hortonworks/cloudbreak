package com.sequenceiq.cloudbreak.ambari;

import static org.mockito.Mockito.mock;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import com.google.common.collect.Lists;
import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.cloudbreak.ambari.flow.AmbariHostsWithNames;
import com.sequenceiq.cloudbreak.domain.stack.Stack;

@RunWith(MockitoJUnitRunner.class)
public class AmbariHostsWithNamesTest {

    @Test
    public void testAmbariHostsWithNames() {
        Stack stack = TestUtil.stack();
        List<String> hostNames = Lists.newArrayList("hostname1", "hostname2");
        AmbariClient ambariClient = mock(AmbariClient.class);

        AmbariHostsWithNames ambariHostsWithNames = new AmbariHostsWithNames(stack, ambariClient, hostNames);

        Assert.assertEquals(stack, ambariHostsWithNames.getStack());
        Assert.assertEquals(hostNames, ambariHostsWithNames.getHostNames());
        Assert.assertEquals(ambariClient, ambariHostsWithNames.getAmbariClient());
    }
}