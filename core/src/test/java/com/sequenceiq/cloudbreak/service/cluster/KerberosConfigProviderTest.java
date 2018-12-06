package com.sequenceiq.cloudbreak.service.cluster;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.domain.KerberosConfig;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;

@RunWith(MockitoJUnitRunner.class)
public class KerberosConfigProviderTest {

    private static final String STACK_NAME = "STACK_NAME";

    @InjectMocks
    private KerberosConfigProvider underTest;

    @Spy
    private Cluster workLoadCluster;

    @Mock
    private Cluster datalakeCluster;

    @Mock
    private Stack datalakeStack;

    @Mock
    private Stack workloadStack;

    @Before
    public void setUp() {
        when(workLoadCluster.getStack()).thenReturn(workloadStack);
        when(datalakeStack.getCluster()).thenReturn(datalakeCluster);
        when(workloadStack.getName()).thenReturn(STACK_NAME);
    }

    @Test
    public void testSetKerberosConfigForWorkloadClusterSuccess() {
        KerberosConfig datalakeKerberosConfig = new KerberosConfig();
        when(datalakeStack.getCluster()).thenReturn(datalakeCluster);
        when(datalakeCluster.getKerberosConfig()).thenReturn(datalakeKerberosConfig);
        underTest.setKerberosConfigForWorkloadCluster(workLoadCluster, datalakeStack);
        assertEquals(workLoadCluster.getKerberosConfig(), datalakeKerberosConfig);
    }

    @Test
    public void testSetKerberosConfigForWorkloadClusterNoDatalakeKerberosConfig() {
        when(datalakeStack.getCluster()).thenReturn(datalakeCluster);
        when(datalakeCluster.getKerberosConfig()).thenReturn(null);
        underTest.setKerberosConfigForWorkloadCluster(workLoadCluster, datalakeStack);
    }

    @Test
    public void testSetKerberosConfigForWorkloadClusterWithoutDatalake() {
        when(datalakeStack.getCluster()).thenReturn(null);
        Throwable exception = assertThrows(BadRequestException.class, () -> underTest.setKerberosConfigForWorkloadCluster(workLoadCluster, datalakeStack));
        assertEquals(exception.getMessage(), String.format("Datalake cluster does not exist for attached cluster %s", STACK_NAME));
    }
}