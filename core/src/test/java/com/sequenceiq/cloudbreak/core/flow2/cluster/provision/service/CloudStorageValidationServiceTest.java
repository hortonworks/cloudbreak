package com.sequenceiq.cloudbreak.core.flow2.cluster.provision.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.common.orchestration.Node;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.host.TelemetryOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.environment.EnvironmentService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakEventService;
import com.sequenceiq.cloudbreak.telemetry.converter.DiagnosticCloudStorageConverter;
import com.sequenceiq.cloudbreak.util.StackUtil;
import com.sequenceiq.common.api.cloudstorage.old.S3CloudStorageV1Parameters;
import com.sequenceiq.common.api.telemetry.response.FeaturesResponse;
import com.sequenceiq.common.api.telemetry.response.LoggingResponse;
import com.sequenceiq.common.api.telemetry.response.TelemetryResponse;
import com.sequenceiq.common.api.type.FeatureSetting;
import com.sequenceiq.common.model.diagnostics.AwsDiagnosticParameters;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;

@ExtendWith(MockitoExtension.class)
class CloudStorageValidationServiceTest {

    private static final String RESOURCE_CRN = "crn:cdp:cloudbreak:us-west-1:default:stack:440ac57e-9f21-4b9a-bcfd-3034a5738b12";

    private static final String ENV_CRN = "envCrn";

    private static final long STACK_ID = 1L;

    private static final String REGION = "region";

    private static final String IDBROKER_HOSTNAME = "fqdn0";

    @Mock
    private TelemetryOrchestrator telemetryOrchestrator;

    @Mock
    private DiagnosticCloudStorageConverter diagnosticCloudStorageConverter;

    @Mock
    private GatewayConfigService gatewayConfigService;

    @Mock
    private StackUtil stackUtil;

    @Mock
    private StackService stackService;

    @Mock
    private EnvironmentService environmentClientService;

    @Mock
    private CloudbreakEventService eventService;

    @Mock
    private EntitlementService entitlementService;

    @InjectMocks
    private CloudStorageValidationService underTest;

    @Test
    void testValidationWithoutEntitlement() throws CloudbreakOrchestratorException {
        Stack stack = getStack();
        when(stackService.getByIdWithClusterInTransaction(STACK_ID)).thenReturn(stack);
        when(entitlementService.cloudStorageValidationOnVmEnabled(anyString())).thenReturn(false);

        underTest.validateCloudStorage(STACK_ID);

        verifyNoInteractions(environmentClientService);
    }

    @Test
    void testValidationWithDisabledCloudStorageLogging() throws CloudbreakOrchestratorException {
        Stack stack = getStack();
        when(stackService.getByIdWithClusterInTransaction(STACK_ID)).thenReturn(stack);
        when(entitlementService.cloudStorageValidationOnVmEnabled(anyString())).thenReturn(true);
        DetailedEnvironmentResponse environment = new DetailedEnvironmentResponse();
        when(environmentClientService.getByCrn(ENV_CRN)).thenReturn(environment);

        underTest.validateCloudStorage(STACK_ID);

        verifyNoInteractions(gatewayConfigService);
    }

    @Test
    void testValidationWithIdbrokerAndRandomHost() throws CloudbreakOrchestratorException {
        Stack stack = getStack();
        when(stackService.getByIdWithClusterInTransaction(STACK_ID)).thenReturn(stack);
        when(entitlementService.cloudStorageValidationOnVmEnabled(anyString())).thenReturn(true);
        DetailedEnvironmentResponse environment = getEnvironment();
        when(environmentClientService.getByCrn(ENV_CRN)).thenReturn(environment);
        List<GatewayConfig> gatewayConfigs = getGatewayConfigs();
        when(gatewayConfigService.getAllGatewayConfigs(stack)).thenReturn(gatewayConfigs);
        Set<Node> nodes = getNodes();
        when(stackUtil.collectNodes(stack)).thenReturn(nodes);
        when(diagnosticCloudStorageConverter.loggingResponseToCloudStorageDiagnosticsParameters(
                environment.getTelemetry().getLogging(), REGION)).thenReturn(new AwsDiagnosticParameters());

        underTest.validateCloudStorage(STACK_ID);

        ArgumentCaptor<Set<String>> targetHostnamesCaptor = ArgumentCaptor.forClass(Set.class);
        verify(telemetryOrchestrator, times(2)).validateCloudStorage(anyList(), anySet(), targetHostnamesCaptor.capture(), anyMap(), any());
        List<Set<String>> capturedTargetHostnames = targetHostnamesCaptor.getAllValues();
        List<String> idbrokerTargets = capturedTargetHostnames.stream()
                .flatMap(Collection::stream)
                .filter(s -> s.equals(IDBROKER_HOSTNAME))
                .collect(Collectors.toList());

        List<String> nonIdbrokerTargets = capturedTargetHostnames.stream()
                .flatMap(Collection::stream)
                .filter(s -> !s.equals(IDBROKER_HOSTNAME))
                .collect(Collectors.toList());

        assertEquals(1, idbrokerTargets.size());
        assertEquals(1, nonIdbrokerTargets.size());
    }

    @Test
    void testValidationWithFailingIdbrokerAndRandomHost() throws CloudbreakOrchestratorException {
        Stack stack = getStack();
        when(stackService.getByIdWithClusterInTransaction(STACK_ID)).thenReturn(stack);
        when(entitlementService.cloudStorageValidationOnVmEnabled(anyString())).thenReturn(true);
        DetailedEnvironmentResponse environment = getEnvironment();
        when(environmentClientService.getByCrn(ENV_CRN)).thenReturn(environment);
        List<GatewayConfig> gatewayConfigs = getGatewayConfigs();
        when(gatewayConfigService.getAllGatewayConfigs(stack)).thenReturn(gatewayConfigs);
        Set<Node> nodes = getNodes();
        when(stackUtil.collectNodes(stack)).thenReturn(nodes);
        when(diagnosticCloudStorageConverter.loggingResponseToCloudStorageDiagnosticsParameters(
                environment.getTelemetry().getLogging(), REGION)).thenReturn(new AwsDiagnosticParameters());
        doThrow(new CloudbreakOrchestratorFailedException("failed")).when(telemetryOrchestrator)
                .validateCloudStorage(anyList(), anySet(), any(), anyMap(), any());

        assertThrows(CloudbreakOrchestratorFailedException.class, () -> underTest.validateCloudStorage(STACK_ID),
                "If provisioning was done using the UI, then verify the log's instance profile and logs location base when provisioning");

        ArgumentCaptor<Set<String>> targetHostnamesCaptor = ArgumentCaptor.forClass(Set.class);
        verify(telemetryOrchestrator, times(2)).validateCloudStorage(anyList(), anySet(), targetHostnamesCaptor.capture(), anyMap(), any());
        verify(eventService, times(1)).fireCloudbreakEvent(anyLong(), anyString(), any());

        List<Set<String>> capturedTargetHostnames = targetHostnamesCaptor.getAllValues();
        List<String> idbrokerTargets = capturedTargetHostnames.stream()
                .flatMap(Collection::stream)
                .filter(s -> s.equals(IDBROKER_HOSTNAME))
                .collect(Collectors.toList());

        List<String> nonIdbrokerTargets = capturedTargetHostnames.stream()
                .flatMap(Collection::stream)
                .filter(s -> !s.equals(IDBROKER_HOSTNAME))
                .collect(Collectors.toList());

        assertEquals(1, idbrokerTargets.size());
        assertEquals(1, nonIdbrokerTargets.size());
    }

    private Stack getStack() {
        Stack stack = new Stack();
        stack.setId(STACK_ID);
        stack.setResourceCrn(RESOURCE_CRN);
        stack.setEnvironmentCrn(ENV_CRN);
        Cluster cluster = new Cluster();
        cluster.setId(2L);
        stack.setCluster(cluster);
        stack.setRegion(REGION);
        return stack;
    }

    private DetailedEnvironmentResponse getEnvironment() {
        DetailedEnvironmentResponse environment = new DetailedEnvironmentResponse();
        TelemetryResponse telemetry = new TelemetryResponse();
        FeaturesResponse features = new FeaturesResponse();
        FeatureSetting featureSetting = new FeatureSetting();
        featureSetting.setEnabled(true);
        features.setCloudStorageLogging(featureSetting);
        telemetry.setFeatures(features);
        LoggingResponse logging = new LoggingResponse();
        logging.setS3(new S3CloudStorageV1Parameters());
        telemetry.setLogging(logging);
        environment.setTelemetry(telemetry);
        return environment;
    }

    private List<GatewayConfig> getGatewayConfigs() {
        return List.of(new GatewayConfig("1", "ip1", "ip1", 1, "i1", true));
    }

    private Set<Node> getNodes() {
        Node idBrokerNode = new Node("ip0", "ip0", "id0", "type0", IDBROKER_HOSTNAME, "idbroker");
        Node node1 = new Node("ip1", "ip1", "id1", "type1", "fqdn1", "master");
        Node node2 = new Node("ip2", "ip2", "id2", "type2", "fqdn2", "worker");
        Node node3 = new Node("ip3", "ip3", "id3", "type3", "fqdn3", "compute");
        return Set.of(idBrokerNode, node1, node2, node3);
    }
}