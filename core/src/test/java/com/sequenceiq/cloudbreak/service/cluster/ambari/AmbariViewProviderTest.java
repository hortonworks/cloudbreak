package com.sequenceiq.cloudbreak.service.cluster.ambari;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.google.common.collect.Lists;
import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.repository.ClusterRepository;

@RunWith(MockitoJUnitRunner.class)
public class AmbariViewProviderTest {

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @Mock
    private ClusterRepository clusterRepository;

    @InjectMocks
    private AmbariViewProvider underTest = new AmbariViewProvider();

    @Test
    public void testProvideViewInformationWhenEverythingWorksFine() {
        Cluster cluster = TestUtil.cluster();
        AmbariClient ambariClient = mock(AmbariClient.class);

        when(clusterRepository.save(cluster)).thenReturn(cluster);
        when(ambariClient.getViewDefinitions()).thenReturn(Lists.newArrayList("test1", "test2"));

        Cluster result = underTest.provideViewInformation(ambariClient, cluster);

        Assert.assertEquals(cluster, result);

        verify(clusterRepository, times(1)).save(any(Cluster.class));
        verify(ambariClient, times(1)).getViewDefinitions();
    }

    @Test
    public void testProvideViewInformationWhenExceptionOccuredThenNoSaveHappensOnRepository() {
        Cluster cluster = TestUtil.cluster();
        AmbariClient ambariClient = mock(AmbariClient.class);

        when(ambariClient.getViewDefinitions()).thenThrow(new AmbariHostsUnavailableException("failed"));

        Cluster result = underTest.provideViewInformation(ambariClient, cluster);

        Assert.assertEquals(cluster, result);

        verify(clusterRepository, times(0)).save(any(Cluster.class));
        verify(ambariClient, times(1)).getViewDefinitions();
    }

}