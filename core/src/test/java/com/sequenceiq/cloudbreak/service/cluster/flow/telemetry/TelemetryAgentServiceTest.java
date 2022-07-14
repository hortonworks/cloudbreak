package com.sequenceiq.cloudbreak.service.cluster.flow.telemetry;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.dto.InstanceGroupDto;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.host.TelemetryOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteriaModel;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;

public class TelemetryAgentServiceTest {

    @InjectMocks
    private TelemetryAgentService underTest;

    @Mock
    private TelemetryOrchestrator telemetryOrchestrator;

    @Mock
    private GatewayConfigService gatewayConfigService;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        underTest = new TelemetryAgentService(telemetryOrchestrator, gatewayConfigService);
    }

    @Test
    public void testStopTelemetryAgent() throws Exception {
        // GIVEN
        doNothing().when(telemetryOrchestrator).stopTelemetryAgent(anyList(), anySet(), any(ExitCriteriaModel.class));
        // WHEN
        underTest.stopTelemetryAgent(createStack());
        // THEN
        verify(telemetryOrchestrator, times(1)).stopTelemetryAgent(anyList(), anySet(), any(ExitCriteriaModel.class));
    }

    @Test
    public void testStopTelemetryAgentThrowsException() throws Exception {
        // GIVEN
        given(gatewayConfigService.getAllGatewayConfigs(any(StackDto.class))).willReturn(null);
        doThrow(new CloudbreakOrchestratorFailedException("error")).when(telemetryOrchestrator)
                .stopTelemetryAgent(anyList(), anySet(), any(ExitCriteriaModel.class));
        // WHEN
        underTest.stopTelemetryAgent(createStack());
        // THEN
        verify(gatewayConfigService, times(1)).getAllGatewayConfigs(any(StackDto.class));
    }

    private StackDto createStack() {
        StackDto stack = mock(StackDto.class);
        when(stack.getId()).thenReturn(1L);
        InstanceGroup instanceGroup = new InstanceGroup();
        instanceGroup.setInstanceMetaData(createInstanceMetadataSet());
        instanceGroup.setTemplate(new Template());
        when(stack.getInstanceGroupDtos()).thenReturn(List.of(new InstanceGroupDto(instanceGroup, new ArrayList<>(instanceGroup.getAllInstanceMetaData()))));
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
