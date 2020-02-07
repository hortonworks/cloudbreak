package com.sequenceiq.cloudbreak.service.cluster.flow.telemetry;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.HashSet;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteriaModel;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;

public class TelemetryAgentServiceTest {

    @InjectMocks
    private TelemetryAgentService underTest;

    @Mock
    private HostOrchestrator hostOrchestrator;

    @Mock
    private GatewayConfigService gatewayConfigService;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        underTest = new TelemetryAgentService(hostOrchestrator, gatewayConfigService);
    }

    @Test
    public void testStopTelemetryAgent() throws Exception {
        // GIVEN
        doNothing().when(hostOrchestrator).stopTelemetryAgent(anyList(), anySet(), any(ExitCriteriaModel.class));
        // WHEN
        underTest.stopTelemetryAgent(createStack());
        // THEN
        verify(hostOrchestrator, times(1)).stopTelemetryAgent(anyList(), anySet(), any(ExitCriteriaModel.class));
    }

    @Test
    public void testStopTelemetryAgentThrowsException() throws Exception {
        // GIVEN
        given(gatewayConfigService.getAllGatewayConfigs(any(Stack.class))).willReturn(null);
        doThrow(new CloudbreakOrchestratorFailedException("error")).when(hostOrchestrator).stopTelemetryAgent(anyList(), anySet(), any(ExitCriteriaModel.class));
        // WHEN
        underTest.stopTelemetryAgent(createStack());
        // THEN
        verify(gatewayConfigService, times(1)).getAllGatewayConfigs(any(Stack.class));
    }

    private Stack createStack() {
        Stack stack = new Stack();
        stack.setId(1L);
        InstanceGroup instanceGroup = new InstanceGroup();
        instanceGroup.setInstanceMetaData(createInstanceMetadataSet());
        instanceGroup.setTemplate(new Template());
        stack.setInstanceGroups(Set.of(instanceGroup));
        return stack;
    }

    private Set<InstanceMetaData> createInstanceMetadataSet() {
        Set<InstanceMetaData> instanceMetaDataSet = new HashSet<>();
        InstanceMetaData instanceMetaData = new InstanceMetaData();
        instanceMetaData.setId(1L);
        InstanceGroup instanceGroup = new InstanceGroup();
        Template template = new Template();
        instanceGroup.setTemplate(template);
        instanceMetaData.setInstanceGroup(instanceGroup);
        instanceMetaDataSet.add(instanceMetaData);
        return instanceMetaDataSet;
    }
}
