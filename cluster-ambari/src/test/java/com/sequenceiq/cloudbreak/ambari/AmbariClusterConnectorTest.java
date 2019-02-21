package com.sequenceiq.cloudbreak.ambari;


import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.client.HttpClientConfig;
import com.sequenceiq.cloudbreak.domain.stack.Stack;

@RunWith(MockitoJUnitRunner.class)
@Ignore
public class AmbariClusterConnectorTest {

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @Mock
    private AmbariClusterSetupService ambariClusterSetupService;

    @Mock
    private AmbariClusterModificationService ambariClusterModificationService;

    @Mock
    private AmbariClusterSecurityService ambariClusterSecurityService;

    private Stack stack = new Stack();

    private HttpClientConfig clientConfig = new HttpClientConfig("1.1.1.1");

    @InjectMocks
    private final AmbariClusterConnector underTest = new AmbariClusterConnector(stack, clientConfig);

    @Test
    public void testClusterModificationServiceShouldReturnWithInjectedBean() {
        Assert.assertEquals(ambariClusterModificationService, underTest.clusterModificationService());
    }

    @Test
    public void testClusterSetupServiceShouldReturnWithInjectedBean() {
        Assert.assertEquals(ambariClusterSetupService, underTest.clusterSetupService());
    }

    @Test
    public void testClusterSecurityServiceShouldReturnWithInjectedBean() {
        Assert.assertEquals(ambariClusterSecurityService, underTest.clusterSecurityService());
    }
}