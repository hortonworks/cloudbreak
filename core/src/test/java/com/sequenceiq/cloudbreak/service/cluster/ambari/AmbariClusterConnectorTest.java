package com.sequenceiq.cloudbreak.service.cluster.ambari;


import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

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

    @InjectMocks
    private AmbariClusterConnector underTest = new AmbariClusterConnector();

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