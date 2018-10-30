package com.sequenceiq.cloudbreak.service.cluster.ambari;

import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Assert;
import org.junit.Before;
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
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.VaultService;

@RunWith(MockitoJUnitRunner.class)
public class AmbariViewProviderTest {

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @Mock
    private ClusterService clusterService;

    @Mock
    private VaultService vaultService;

    @InjectMocks
    private final AmbariViewProvider underTest = new AmbariViewProvider();

    @Before
    public void setUp() {
        when(vaultService.resolveSingleValue(anyString())).then(returnsFirstArg());
    }

    @Test
    public void testProvideViewInformationWhenEverythingWorksFine() {
        Cluster cluster = TestUtil.cluster();
        AmbariClient ambariClient = mock(AmbariClient.class);

        when(clusterService.save(cluster)).thenReturn(cluster);
        when(ambariClient.getViewDefinitions()).thenReturn(Lists.newArrayList("test1", "test2"));

        Cluster result = underTest.provideViewInformation(ambariClient, cluster);

        Assert.assertEquals(cluster, result);

        verify(clusterService, times(1)).save(any(Cluster.class));
        verify(ambariClient, times(1)).getViewDefinitions();
    }

    @Test
    public void testProvideViewInformationWhenExceptionOccuredThenNoSaveHappensOnRepository() {
        Cluster cluster = TestUtil.cluster();
        AmbariClient ambariClient = mock(AmbariClient.class);

        when(ambariClient.getViewDefinitions()).thenThrow(new AmbariHostsUnavailableException("failed"));

        Cluster result = underTest.provideViewInformation(ambariClient, cluster);

        Assert.assertEquals(cluster, result);

        verify(clusterService, times(0)).save(any(Cluster.class));
        verify(ambariClient, times(1)).getViewDefinitions();
    }

}