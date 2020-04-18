package com.sequenceiq.periscope.monitor.evaluator.cm;

import static com.sequenceiq.periscope.api.model.ClusterState.PENDING;
import static com.sequenceiq.periscope.api.model.ClusterState.RUNNING;
import static com.sequenceiq.periscope.api.model.ClusterState.SUSPENDED;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloudera.api.swagger.ClouderaManagerResourceApi;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.AutoscaleStackV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.instancemetadata.InstanceMetaDataV4Response;
import com.sequenceiq.cloudbreak.cm.client.ClouderaManagerApiClientProvider;
import com.sequenceiq.cloudbreak.cm.client.retry.ClouderaManagerApiFactory;
import com.sequenceiq.cloudbreak.service.secret.service.SecretService;
import com.sequenceiq.periscope.api.model.ClusterState;
import com.sequenceiq.periscope.api.model.ScalingStatus;
import com.sequenceiq.periscope.aspects.RequestLogging;
import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.domain.ClusterPertain;
import com.sequenceiq.periscope.domain.History;
import com.sequenceiq.periscope.domain.SecurityConfig;
import com.sequenceiq.periscope.model.MonitoredStack;
import com.sequenceiq.periscope.monitor.context.EvaluatorContext;
import com.sequenceiq.periscope.notification.HttpNotificationSender;
import com.sequenceiq.periscope.service.ClusterService;
import com.sequenceiq.periscope.service.HistoryService;
import com.sequenceiq.periscope.service.security.SecurityConfigService;
import com.sequenceiq.periscope.service.security.TlsHttpClientConfigurationService;
import com.sequenceiq.periscope.service.security.TlsSecurityService;

@RunWith(MockitoJUnitRunner.class)
public class ClouderaManagerClusterCreationEvaluatorTest {

    private static final long STACK_ID = 1;

    private static final String STACK_CRN = "STACK_CRN";

    @Captor
    public ArgumentCaptor<ClusterPertain> captor;

    @Mock
    private ClusterService clusterService;

    @Mock
    private ClouderaManagerApiClientProvider clouderaManagerApiClientProvider;

    @Mock
    private ClouderaManagerApiFactory clouderaManagerApiFactory;

    @Mock
    private TlsSecurityService tlsSecurityService;

    @Mock
    private TlsHttpClientConfigurationService tlsHttpClientConfigurationService;

    @Mock
    private SecurityConfigService securityConfigService;

    @Mock
    private HistoryService historyService;

    @Mock
    private HttpNotificationSender notificationSender;

    @Mock
    private RequestLogging requestLogging;

    @Mock
    private SecretService secretService;

    @InjectMocks
    private ClouderaManagerClusterCreationEvaluator underTest;

    @Mock
    private EvaluatorContext evaluatorContext;

    @Before
    public void setUp() {
        underTest.setContext(evaluatorContext);
    }

    @Test
    public void shouldUpdateSuspendedHelthyCluster() {
        Cluster cluster = getCluster(SUSPENDED);
        History history = new History();
        StackV4Response stack = new StackV4Response();

        setUpMocks(cluster, true, stack, history);

        underTest.execute();

        verify(clusterService).findOneByStackId(STACK_ID);
        verify(clusterService).update(eq(cluster.getId()), any(), eq(RUNNING), eq(true));
        verify(historyService).createEntry(ScalingStatus.ENABLED, "Autoscaling has been enabled for the cluster.", 0, cluster);
        verify(notificationSender).send(cluster, history);
    }

    @Test
    public void shouldUpdatePendingHelthyCluster() {
        Cluster cluster = getCluster(PENDING);
        History history = new History();
        StackV4Response stack = new StackV4Response();

        setUpMocks(cluster, true, stack, history);

        underTest.execute();

        verify(clusterService).findOneByStackId(STACK_ID);
        verify(clusterService).update(eq(cluster.getId()), any(), eq(RUNNING), eq(true));
        verify(historyService).createEntry(ScalingStatus.ENABLED, "Autoscaling has been enabled for the cluster.", 0, cluster);
        verify(notificationSender).send(cluster, history);
    }

    @Test
    public void shouldNotUpdateRunningHelthyCluster() {
        Cluster cluster = getCluster(RUNNING);
        History history = new History();
        StackV4Response stack = new StackV4Response();

        setUpMocks(cluster, true, stack, history);

        underTest.execute();

        verify(clusterService).findOneByStackId(STACK_ID);
        verifyNoMoreInteractions(clusterService);
        verifyZeroInteractions(historyService);
        verifyZeroInteractions(notificationSender);
    }

    @Test
    public void shouldNotUpdateSuspendedUnHelthyCluster() {
        Cluster cluster = getCluster(SUSPENDED);
        History history = new History();
        StackV4Response stack = new StackV4Response();

        setUpMocks(cluster, false, stack, history);

        underTest.execute();

        verify(clusterService).findOneByStackId(STACK_ID);
        verifyNoMoreInteractions(clusterService);
        verifyZeroInteractions(historyService);
        verifyZeroInteractions(notificationSender);
    }

    @Test
    public void shouldValidateAndCreateNewCluster() {
        History history = new History();
        StackV4Response stack = new StackV4Response();

        setUpMocks(null, false, stack, history);

        Cluster cluster = getCluster(null);
        when(clusterService.create(any(), any(), any())).thenReturn(cluster);
        when(historyService.createEntry(any(), anyString(), anyInt(), eq(cluster))).thenReturn(history);

        underTest.execute();

        verify(clusterService).findOneByStackId(STACK_ID);
        verify(clusterService).validateClusterUniqueness(any());
        verify(clusterService).create(any(MonitoredStack.class), eq(RUNNING), captor.capture());
        ClusterPertain clusterPertain = captor.getValue();
        assertThat(clusterPertain.getTenant(), is("TENANT"));
        assertThat(clusterPertain.getWorkspaceId(), is(10L));
        assertThat(clusterPertain.getUserId(), is("USER_ID"));
    }

    private void setUpMocks(Cluster cluster, boolean healthy, StackV4Response stackV4Response, History history) {
        InstanceMetaDataV4Response instanceMetaData = new InstanceMetaDataV4Response();
        instanceMetaData.setDiscoveryFQDN("master");
        setUpMocks(cluster, healthy, stackV4Response, Optional.of(instanceMetaData), history);
    }

    private void setUpMocks(Cluster cluster, boolean healthy, StackV4Response stackV4Response, Optional<InstanceMetaDataV4Response> primaryGateways,
            History history) {
        AutoscaleStackV4Response stack = getStackResponse();
        when(evaluatorContext.getData()).thenReturn(stack);
        when(securityConfigService.getSecurityConfig(anyLong())).thenReturn(new SecurityConfig());
        when(clusterService.findOneByStackId(anyLong())).thenReturn(cluster);
        when(requestLogging.logResponseTime(any(), any())).thenReturn(healthy);
        if (cluster != null) {
            when(historyService.createEntry(any(), anyString(), anyInt(), any(Cluster.class))).thenReturn(history);
        }
        when(clusterService.update(anyLong(), any(), any(), anyBoolean())).thenReturn(cluster);
        // CHECKSTYLE:OFF
        when(clouderaManagerApiFactory.getClouderaManagerResourceApi(any())).thenReturn(new ClouderaManagerResourceApi());
        // CHECKSTYLE:ON
    }

    private AutoscaleStackV4Response getStackResponse() {
        AutoscaleStackV4Response stack = new AutoscaleStackV4Response();
        stack.setStackId(STACK_ID);
        stack.setStackCrn(STACK_CRN);
        stack.setAmbariServerIp("0.0.0.0");
        stack.setGatewayPort(8080);
        stack.setTenant("TENANT");
        stack.setWorkspaceId(10L);
        stack.setUserId("USER_ID");
        return stack;
    }

    private Cluster getCluster(ClusterState clusterState) {
        Cluster cluster = new Cluster();
        cluster.setId(10);
        cluster.setStackCrn(STACK_CRN);
        cluster.setAutoscalingEnabled(true);
        cluster.setState(clusterState);
        return cluster;
    }

}