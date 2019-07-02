package com.sequenceiq.cloudbreak.service.cluster;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostMetadata;
import com.sequenceiq.cloudbreak.exception.NotFoundException;
import com.sequenceiq.cloudbreak.service.ClusterCommonService;
import com.sequenceiq.cloudbreak.service.hostmetadata.HostMetadataService;

@RunWith(MockitoJUnitRunner.class)
public class ClusterCommonServiceTest {

    @Mock
    private HostMetadataService hostMetadataService;

    @InjectMocks
    private ClusterCommonService underTest;

    @Before
    public void setUp() {
    }

    @Test
    public void testIniFileGeneration() {
        // GIVEN
        Cluster cluster = new Cluster();
        cluster.setId(1L);
        cluster.setName("cl1");
        cluster.setAmbariIp("gatewayIP");

        when(hostMetadataService.findHostsInCluster(1L)).thenReturn(generateHostMetadata());

        // WHEN
        String result = underTest.getHostNamesAsIniString(cluster, "cloudbreak");
        // THEN
        verify(hostMetadataService).findHostsInCluster(1L);
        assertTrue(result.contains("[server]\ngatewayIP\n\n"));
        assertTrue(result.contains("[cluster]\nname=cl1\n\n"));
        assertTrue(result.contains("[master]\nh1\n"));
        assertTrue(result.contains("[agent]\n"));
        assertTrue(result.contains("[all:vars]\nansible_ssh_user=cloudbreak\n"));
    }

    @Test(expected = NotFoundException.class)
    public void testIniFileGenerationWithoutAgents() {
        Cluster cluster = new Cluster();
        cluster.setId(1L);
        cluster.setName("cl1");
        cluster.setAmbariIp(null);

        when(hostMetadataService.findHostsInCluster(1L)).thenReturn(new HashSet<>());
        // WHEN
        underTest.getHostNamesAsIniString(cluster, "cloudbreak");
    }

    private Set<HostMetadata> generateHostMetadata() {
        Set<HostMetadata> hostMetadataSet = new HashSet<>();
        HostGroup master = new HostGroup();
        master.setName("master");
        HostGroup worker = new HostGroup();
        worker.setName("worker");
        HostMetadata h1 = new HostMetadata();
        h1.setHostName("h1");
        h1.setHostGroup(master);
        HostMetadata h2 = new HostMetadata();
        h2.setHostName("h2");
        h2.setHostGroup(worker);
        HostMetadata h3 = new HostMetadata();
        h3.setHostName("h3");
        h3.setHostGroup(worker);
        hostMetadataSet.add(h1);
        hostMetadataSet.add(h2);
        hostMetadataSet.add(h3);
        return hostMetadataSet;
    }

}
