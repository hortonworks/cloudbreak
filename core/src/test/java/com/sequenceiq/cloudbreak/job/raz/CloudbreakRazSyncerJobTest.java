package com.sequenceiq.cloudbreak.job.raz;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cluster.api.ClusterApi;
import com.sequenceiq.cloudbreak.cluster.api.ClusterModificationService;
import com.sequenceiq.cloudbreak.cluster.api.ClusterStatusService;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.StackStatus;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.quartz.raz.service.RazSyncerJobService;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.environment.EnvironmentClientService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.stack.StackViewService;
import com.sequenceiq.cloudbreak.workspace.model.User;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.flow.core.FlowLogService;
import com.sequenceiq.sdx.api.endpoint.SdxEndpoint;

import io.opentracing.Tracer;

@ExtendWith(MockitoExtension.class)
class CloudbreakRazSyncerJobTest {

    @InjectMocks
    private CloudbreakRazSyncerJob underTest;

    @Mock
    private StackViewService stackViewService;

    @Mock
    private StackService stackService;

    @Mock
    private RazSyncerJobService jobService;

    @Mock
    private JobExecutionContext jobExecutionContext;

    @Mock
    private SdxEndpoint sdxEndpoint;

    @Mock
    private ClusterApiConnectors apiConnectors;

    @Mock
    private ClusterApi clusterApi;

    @Mock
    private EntitlementService entitlementService;

    @Mock
    private ClusterService clusterService;

    @Mock
    private ClusterStatusService clusterStatusService;

    @Mock
    private ClusterModificationService clusterModificationService;

    @Mock
    private FlowLogService flowLogService;

    @Mock
    private EnvironmentClientService environmentClientService;

    @Mock
    private DetailedEnvironmentResponse environmentResponse;

    @Mock
    private Cluster cluster;

    private Stack stack;

    private User user;

    private Workspace workspace;

    @BeforeEach
    public void setUp() throws Exception {
        Tracer tracer = Mockito.mock(Tracer.class);
        underTest = new CloudbreakRazSyncerJob(tracer);
        MockitoAnnotations.openMocks(this);
        when(flowLogService.isOtherFlowRunning(anyLong())).thenReturn(false);
        underTest.setLocalId("1");
        underTest.setRemoteResourceCrn("test-remote-crn");

        stack = new Stack();
        stack.setId(1L);
        stack.setResourceCrn("test-stack-crn");
        workspace = new Workspace();
        workspace.setId(1L);
        stack.setWorkspace(workspace);
        stack.setCluster(cluster);
        user = new User();
        user.setUserId("1");
        stack.setCreator(user);
        stack.setType(StackType.DATALAKE);
        stack.setEnvironmentCrn("test-env-crn");
        stack.setStackVersion("7.2.11");
    }

    @Test
    void testDoesNotExecuteIfOtherFlowIsRunning() throws JobExecutionException {
        when(flowLogService.isOtherFlowRunning(anyLong())).thenReturn(true);

        underTest.executeTracedJob(jobExecutionContext);

        verify(stackService, times(0)).get(anyLong());
    }

    @Test
    void testSyncIsSkippedIfStatusNotInUnschedulableOrSyncable() throws JobExecutionException {
        stack.setStackStatus(new StackStatus(stack, DetailedStackStatus.STOPPED));
        when(stackService.get(anyLong())).thenReturn(stack);

        underTest.executeTracedJob(jobExecutionContext);

        verify(apiConnectors, times(0)).getConnector(any(Stack.class));
    }

    @Test
    void testJobUnscheduledIfStackIsDeleted() throws JobExecutionException {
        stack.setStackStatus(new StackStatus(stack, DetailedStackStatus.DELETE_COMPLETED));
        when(stackService.get(anyLong())).thenReturn(stack);

        underTest.executeTracedJob(jobExecutionContext);

        verify(jobService, times(1)).unschedule();
    }

    @Test
    void testJobRunsWhenCMIsRunning() throws JobExecutionException {
        stack.setStackStatus(new StackStatus(stack, DetailedStackStatus.AVAILABLE));
        when(stackService.get(anyLong())).thenReturn(stack);
        when(apiConnectors.getConnector(stack)).thenReturn(clusterApi);
        when(clusterApi.clusterStatusService()).thenReturn(clusterStatusService);
        when(clusterApi.clusterModificationService()).thenReturn(clusterModificationService);
        when(clusterStatusService.isClusterManagerRunningQuickCheck()).thenReturn(true);
        when(cluster.getName()).thenReturn("test-cluster-name");
        when(clusterModificationService.isServicePresent(anyString(), anyString())).thenReturn(true);
        when(entitlementService.razEnabled(anyString())).thenReturn(true);
        when(environmentClientService.getByCrn(anyString())).thenReturn(environmentResponse);
        when(environmentResponse.getCloudPlatform()).thenReturn("AWS");
        when(environmentResponse.getCreator()).thenReturn("crn:cdp:iam:us-west-1:tenant:user:username");

        underTest.executeTracedJob(jobExecutionContext);

        verify(sdxEndpoint, times(1)).enableRangerRazByCrn(anyString());
        verify(clusterService, times(1)).save(any(Cluster.class));
    }

    @Test
    void testEndpointIsNotInvokedIfRazFlagAlreadySet() throws JobExecutionException {
        stack.setStackStatus(new StackStatus(stack, DetailedStackStatus.AVAILABLE));
        when(stackService.get(anyLong())).thenReturn(stack);
        when(apiConnectors.getConnector(stack)).thenReturn(clusterApi);
        when(clusterApi.clusterStatusService()).thenReturn(clusterStatusService);
        when(clusterStatusService.isClusterManagerRunningQuickCheck()).thenReturn(true);
        when(cluster.isRangerRazEnabled()).thenReturn(true);

        underTest.executeTracedJob(jobExecutionContext);

        verify(sdxEndpoint, times(0)).enableRangerRazByCrn(anyString());
        verify(clusterService, times(0)).save(any(Cluster.class));
    }
}