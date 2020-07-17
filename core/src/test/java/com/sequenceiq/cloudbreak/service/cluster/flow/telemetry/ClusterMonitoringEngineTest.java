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
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.host.TelemetryOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteriaModel;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.common.api.telemetry.model.Features;
import com.sequenceiq.common.api.telemetry.model.Telemetry;

public class ClusterMonitoringEngineTest {

    @InjectMocks
    private ClusterMonitoringEngine underTest;

    @Mock
    private TelemetryOrchestrator telemetryOrchestrator;

    @Mock
    private GatewayConfigService gatewayConfigService;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        underTest = new ClusterMonitoringEngine(telemetryOrchestrator, gatewayConfigService);
    }

    @Test
    public void testInstallAndStartMonitoring() throws Exception {
        // GIVEN
        doNothing().when(telemetryOrchestrator).installAndStartMonitoring(anyList(), anySet(), any(ExitCriteriaModel.class));
        // WHEN
        underTest.installAndStartMonitoring(createStack(), createTelemetry());
        // THEN
        verify(telemetryOrchestrator, times(1)).installAndStartMonitoring(anyList(), anySet(), any(ExitCriteriaModel.class));
    }

    @Test
    public void testInstallAndStartMonitoringThrowsException() throws Exception {
        // GIVEN
        given(gatewayConfigService.getAllGatewayConfigs(any(Stack.class))).willReturn(null);
        doThrow(new CloudbreakOrchestratorFailedException("error")).when(telemetryOrchestrator).installAndStartMonitoring(
                anyList(), anySet(), any(ExitCriteriaModel.class));
        // WHEN
        underTest.installAndStartMonitoring(createStack(), createTelemetry());
        // THEN
        verify(gatewayConfigService, times(1)).getAllGatewayConfigs(any(Stack.class));
    }

    private Telemetry createTelemetry() {
        Telemetry telemetry = new Telemetry();
        Features features = new Features();
        features.addMonitoring(true);
        telemetry.setFeatures(features);
        return telemetry;
    }

    private Stack createStack() {
        Stack stack = new Stack();
        stack.setId(1L);
        InstanceGroup instanceGroup = new InstanceGroup();
        instanceGroup.setInstanceMetaData(createInstanceMetadataSet());
        instanceGroup.setTemplate(new Template());
        stack.setInstanceGroups(Set.of(instanceGroup));
        Cluster cluster = new Cluster();
        cluster.setId(1L);
        stack.setCluster(cluster);
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
