package com.sequenceiq.cloudbreak.service.cluster;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.domain.KerberosConfig;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.DatalakeResources;

@RunWith(MockitoJUnitRunner.class)
public class KerberosConfigProviderTest {

    @InjectMocks
    private KerberosConfigProvider underTest;

    @Test
    public void testSetKerberosConfigForWorkloadClusterSuccess() {
        Cluster workLoadCluster = new Cluster();
        KerberosConfig datalakeKerberosConfig = new KerberosConfig();
        DatalakeResources datalakeResources = new DatalakeResources();
        datalakeResources.setKerberosConfig(datalakeKerberosConfig);
        underTest.setKerberosConfigForWorkloadCluster(workLoadCluster, datalakeResources);
        assertEquals(workLoadCluster.getKerberosConfig(), datalakeKerberosConfig);
    }
}