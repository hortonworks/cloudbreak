package com.sequenceiq.cloudbreak.ambari;


import static org.mockito.Mockito.when;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.context.ApplicationContext;

import com.sequenceiq.cloudbreak.ambari.status.AmbariClusterStatusService;
import com.sequenceiq.cloudbreak.client.HttpClientConfig;
import com.sequenceiq.cloudbreak.domain.stack.Stack;

@RunWith(MockitoJUnitRunner.class)
public class AmbariClusterConnectorTest {

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @Mock
    private AmbariClusterSetupService ambariClusterSetupService;

    @Mock
    private AmbariClusterModificationService ambariClusterModificationService;

    @Mock
    private AmbariClusterSecurityService ambariClusterSecurityService;

    @Mock
    private AmbariClusterStatusService ambariClusterStatusService;

    @Mock
    private AmbariClusterDecomissionService ambariClusterDecomissionService;

    @Mock
    private ApplicationContext applicationContext;

    private Stack stack = new Stack();

    private HttpClientConfig clientConfig = new HttpClientConfig("1.1.1.1");

    @InjectMocks
    private final AmbariClusterConnector underTest = new AmbariClusterConnector(stack, clientConfig);

    @Test
    public void testClusterModificationServiceShouldReturnWithInjectedBean() {
        when(applicationContext.getBean(AmbariClusterModificationService.class, stack, clientConfig)).thenReturn(ambariClusterModificationService);

        Assert.assertEquals(ambariClusterModificationService, underTest.clusterModificationService());
    }

    @Test
    public void testClusterSetupServiceShouldReturnWithInjectedBean() {
        when(applicationContext.getBean(AmbariClusterSetupService.class, stack, clientConfig)).thenReturn(ambariClusterSetupService);

        Assert.assertEquals(ambariClusterSetupService, underTest.clusterSetupService());
    }

    @Test
    public void testClusterSecurityServiceShouldReturnWithInjectedBean() {
        when(applicationContext.getBean(AmbariClusterSecurityService.class, stack, clientConfig)).thenReturn(ambariClusterSecurityService);

        Assert.assertEquals(ambariClusterSecurityService, underTest.clusterSecurityService());
    }

    @Test
    public void testClusterStatusServiceShouldReturnWithInjectedBean() {
        when(applicationContext.getBean(AmbariClusterStatusService.class, stack, clientConfig)).thenReturn(ambariClusterStatusService);

        Assert.assertEquals(ambariClusterStatusService, underTest.clusterStatusService());
    }

    @Test
    public void testClusterDecomissionServiceShouldReturnWithInjectedBean() {
        when(applicationContext.getBean(AmbariClusterDecomissionService.class, stack, clientConfig)).thenReturn(ambariClusterDecomissionService);

        Assert.assertEquals(ambariClusterDecomissionService, underTest.clusterDecomissionService());
    }
}