package com.sequenceiq.cloudbreak.telemetry.upgrade;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.HashMap;
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

import com.sequenceiq.cloudbreak.common.orchestration.Node;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.host.TelemetryOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.metadata.OrchestratorMetadata;
import com.sequenceiq.cloudbreak.orchestrator.metadata.OrchestratorMetadataProvider;
import com.sequenceiq.cloudbreak.telemetry.TelemetryComponentType;
import com.sequenceiq.cloudbreak.telemetry.TelemetryComponentUpgradeConfiguration;
import com.sequenceiq.cloudbreak.telemetry.TelemetryUpgradeConfiguration;
import com.sequenceiq.cloudbreak.telemetry.orchestrator.TelemetryConfigProvider;
import com.sequenceiq.cloudbreak.util.CompressUtil;

@ExtendWith(MockitoExtension.class)
public class TelemetryUpgradeServiceTest {

    private static final Long STACK_ID = 1L;

    private static final List<String> SALT_BASE_FOLDERS = List.of("salt-common", "salt");

    @InjectMocks
    private TelemetryUpgradeService underTest;

    @Mock
    private TelemetryConfigProvider telemetryConfigProvider;

    @Mock
    private CompressUtil compressUtil;

    @Mock
    private OrchestratorMetadataProvider orchestratorMetadataProvider;

    @Mock
    private TelemetryOrchestrator telemetryOrchestrator;

    @Mock
    private HostOrchestrator hostOrchestrator;

    @Mock
    private OrchestratorMetadata metadata;

    @Mock
    private TelemetryUpgradeConfiguration telemetryUpgradeConfiguration;

    @BeforeEach
    public void setUp() {
        underTest = new TelemetryUpgradeService();
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testUpgradeTelemetrySaltStates() throws Exception {
        // GIVEN
        given(orchestratorMetadataProvider.getSaltStateDefinitionBaseFolders()).willReturn(SALT_BASE_FOLDERS);
        given(orchestratorMetadataProvider.getStoredStates(STACK_ID)).willReturn(new byte[0]);
        given(compressUtil.generateCompressedOutputFromFolders(anyList(), anyList())).willReturn(new byte[0]);
        given(orchestratorMetadataProvider.getOrchestratorMetadata(STACK_ID)).willReturn(metadata);
        given(compressUtil.compareCompressedContent(any(), any(), anyList())).willReturn(false);
        doNothing().when(telemetryOrchestrator).updatePartialSaltDefinition(any(), anyList(), anyList(), isNull());
        // WHEN
        underTest.upgradeTelemetrySaltStates(STACK_ID, Set.of(TelemetryComponentType.CDP_TELEMETRY));
        // THEN
        verify(telemetryOrchestrator, times(1)).updatePartialSaltDefinition(any(), anyList(), anyList(), isNull());
        verify(orchestratorMetadataProvider, times(1)).storeNewState(anyLong(), any());
    }

    @Test
    public void testUpgradeTelemetrySaltStatesWithNotChangedState() throws Exception {
        // GIVEN
        given(orchestratorMetadataProvider.getSaltStateDefinitionBaseFolders()).willReturn(SALT_BASE_FOLDERS);
        given(orchestratorMetadataProvider.getStoredStates(STACK_ID)).willReturn(new byte[0]);
        given(compressUtil.generateCompressedOutputFromFolders(anyList(), anyList())).willReturn(new byte[0]);
        given(compressUtil.compareCompressedContent(any(), any(), anyList())).willReturn(true);
        // WHEN
        underTest.upgradeTelemetrySaltStates(STACK_ID, Set.of(TelemetryComponentType.CDP_TELEMETRY));
        // THEN
        verify(compressUtil, times(1)).compareCompressedContent(any(), any(), anyList());
        verify(telemetryOrchestrator, times(0)).updatePartialSaltDefinition(any(), anyList(), anyList(), any());
        verify(orchestratorMetadataProvider, times(0)).storeNewState(anyLong(), any());
    }

    @Test
    public void testUpgradeTelemetrySaltStatesWithoutExistingSaltState() throws Exception {
        // GIVEN
        given(orchestratorMetadataProvider.getSaltStateDefinitionBaseFolders()).willReturn(SALT_BASE_FOLDERS);
        given(orchestratorMetadataProvider.getStoredStates(STACK_ID)).willReturn(null);
        given(compressUtil.generateCompressedOutputFromFolders(anyList(), anyList())).willReturn(null);
        given(orchestratorMetadataProvider.getOrchestratorMetadata(STACK_ID)).willReturn(metadata);
        doNothing().when(telemetryOrchestrator).updatePartialSaltDefinition(isNull(), anyList(), anyList(), any());
        // WHEN
        underTest.upgradeTelemetrySaltStates(STACK_ID, Set.of(TelemetryComponentType.CDP_TELEMETRY));
        // THEN
        verify(telemetryOrchestrator, times(1)).updatePartialSaltDefinition(isNull(), anyList(), anyList(), any());
        verify(orchestratorMetadataProvider, times(0)).storeNewState(anyLong(), any());
    }

    @Test
    public void testUpdateTelemetryComponent() throws Exception {
        // GIVEN
        given(orchestratorMetadataProvider.getOrchestratorMetadata(STACK_ID)).willReturn(metadata);
        TelemetryComponentUpgradeConfiguration telemetryConfig = new TelemetryComponentUpgradeConfiguration();
        telemetryConfig.setDesiredVersion("0.1.0");
        given(telemetryUpgradeConfiguration.getCdpTelemetry()).willReturn(telemetryConfig);
        doNothing().when(telemetryOrchestrator).updateTelemetryComponent(anyList(), anySet(), any(), anyMap());
        // WHEN
        underTest.upgradeTelemetryComponent(STACK_ID, TelemetryComponentType.CDP_TELEMETRY, null);
        // THEN
        verify(telemetryUpgradeConfiguration, times(1)).getCdpTelemetry();
        verify(orchestratorMetadataProvider, times(1)).getOrchestratorMetadata(STACK_ID);
        verify(telemetryOrchestrator, times(1))
                .updateTelemetryComponent(anyList(), anySet(), any(), anyMap());
    }

    @Test
    public void testUpdateLoggingAgentComponent() throws Exception {
        // GIVEN
        given(orchestratorMetadataProvider.getOrchestratorMetadata(STACK_ID)).willReturn(metadata);
        TelemetryComponentUpgradeConfiguration loggingAgentConfig = new TelemetryComponentUpgradeConfiguration();
        loggingAgentConfig.setDesiredVersion("0.1.0");
        given(telemetryUpgradeConfiguration.getCdpLoggingAgent()).willReturn(loggingAgentConfig);
        doNothing().when(telemetryOrchestrator).updateTelemetryComponent(anyList(), anySet(), any(), anyMap());
        // WHEN
        underTest.upgradeTelemetryComponent(STACK_ID, TelemetryComponentType.CDP_LOGGING_AGENT, null);
        // THEN
        verify(telemetryUpgradeConfiguration, times(1)).getCdpLoggingAgent();
        verify(orchestratorMetadataProvider, times(1)).getOrchestratorMetadata(STACK_ID);
        verify(telemetryOrchestrator, times(1))
                .updateTelemetryComponent(anyList(), anySet(), any(), anyMap());
    }

    @Test
    public void testUpgradeTelemetrySaltPillars() throws Exception {
        // GIVEN
        Set<Node> nodes = nodes();
        Set<TelemetryComponentType> componentTypes = Set.of(TelemetryComponentType.CDP_TELEMETRY);
        given(telemetryConfigProvider.createTelemetryConfigs(STACK_ID, componentTypes)).willReturn(new HashMap<>());
        given(orchestratorMetadataProvider.getOrchestratorMetadata(STACK_ID)).willReturn(metadata);
        given(metadata.getNodes()).willReturn(nodes);
        given(telemetryOrchestrator.collectUnresponsiveNodes(anyList(), anySet(), any())).willReturn(new HashSet<>());
        doNothing().when(hostOrchestrator).initSaltConfig(any(), anyList(), anySet(), any(), any());
        // WHEN
        underTest.upgradeTelemetrySaltPillars(STACK_ID, componentTypes);
        // THEN
        verify(hostOrchestrator, times(1)).initSaltConfig(any(), anyList(), anySet(), any(), any());
    }

    @Test
    public void testUpgradeTelemetrySaltPillarsWithUnresponsiveNodes() throws Exception {
        // GIVEN
        Set<Node> nodes = nodes();
        Set<TelemetryComponentType> componentTypes = Set.of(TelemetryComponentType.CDP_TELEMETRY);
        given(orchestratorMetadataProvider.getOrchestratorMetadata(STACK_ID)).willReturn(metadata);
        given(metadata.getNodes()).willReturn(nodes);
        given(telemetryOrchestrator.collectUnresponsiveNodes(anyList(), anySet(), any())).willReturn(nodes);
        // WHEN
        CloudbreakOrchestratorFailedException result = assertThrows(CloudbreakOrchestratorFailedException.class,
                () -> underTest.upgradeTelemetrySaltPillars(STACK_ID, componentTypes));
        // THEN
        assertTrue(result.getMessage().contains("Not found any available nodes"));
    }

    private Set<Node> nodes() {
        Set<Node> nodes = new HashSet<>();
        nodes.add(new Node("privateIp", "publicIp", null, null));
        return nodes;
    }
}
