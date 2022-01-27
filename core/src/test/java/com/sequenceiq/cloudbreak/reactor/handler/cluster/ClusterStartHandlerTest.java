package com.sequenceiq.cloudbreak.reactor.handler.cluster;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cluster.api.ClusterApi;
import com.sequenceiq.cloudbreak.cluster.api.ClusterCommissionService;
import com.sequenceiq.cloudbreak.cluster.api.ClusterStatusService;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.util.StackUtil;

@ExtendWith(MockitoExtension.class)
class ClusterStartHandlerTest {

    @Mock
    private CmTemplateProcessor cmTemplateProcessor;

    @Mock
    private StackUtil stackUtil;

    @Mock
    private ClusterApiConnectors apiConnectors;

    @Mock
    private ClusterApi connector;

    @Mock
    private ClusterStatusService clusterStatusService;

    @Mock
    private ClusterCommissionService clusterCommissionService;

    @InjectMocks
    private ClusterStartHandler underTest;

    private Stack stack;

    @BeforeEach
    void setUp() {
        underTest = new ClusterStartHandler();
        MockitoAnnotations.openMocks(this);

        stack = new Stack();
        stack.setCloudPlatform("AWS");

        when(stackUtil.stopStartScalingEntitlementEnabled(any())).thenReturn(true);
        when(apiConnectors.getConnector(any(Stack.class))).thenReturn(connector);
        when(connector.clusterStatusService()).thenReturn(clusterStatusService);
        lenient().when(connector.clusterCommissionService()).thenReturn(clusterCommissionService);
        List<String> decommHosts = new ArrayList<>();
        decommHosts.add("computing-computing0.foo.bar");
        decommHosts.add("computing-compute0.foo.bar");
        decommHosts.add("computing-working0.foo.bar");
        when(clusterStatusService.getDecommissionedHostsFromCM()).thenReturn(decommHosts);
        Set<String> computeGroups = new HashSet<>();
        computeGroups.add("compute");
        computeGroups.add("computing");
        when(cmTemplateProcessor.getComputeHostGroups(any())).thenReturn(computeGroups);
    }

    @Test
    void stopStartScalingFeatureShouldRecommissionComputeGroups() {
        underTest.handleStopStartScalingFeature(stack, cmTemplateProcessor);
        List<String> recommHosts = new ArrayList<>();
        recommHosts.add("computing-computing0.foo.bar");
        recommHosts.add("computing-compute0.foo.bar");
        verify(clusterCommissionService, times(1)).recommissionHosts(eq(recommHosts));
    }

    @Test
    void stopStartScalingFeatureShouldNotRecommissionNonComputeGroups() {
        List<String> decommHosts = new ArrayList<>();
        decommHosts.add("computing-working0.foo.bar");
        when(clusterStatusService.getDecommissionedHostsFromCM()).thenReturn(decommHosts);
        underTest.handleStopStartScalingFeature(stack, cmTemplateProcessor);
        verify(clusterCommissionService, times(0)).recommissionHosts(any());
    }
}