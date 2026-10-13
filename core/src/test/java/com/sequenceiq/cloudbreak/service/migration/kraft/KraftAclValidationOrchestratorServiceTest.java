package com.sequenceiq.cloudbreak.service.migration.kraft;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessorFactory;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.kafka.KafkaRoles;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.orchestration.Node;
import com.sequenceiq.cloudbreak.core.bootstrap.service.host.ClusterHostServiceRunner;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.host.OrchestratorStateParams;
import com.sequenceiq.cloudbreak.orchestrator.model.NodeReachabilityResult;
import com.sequenceiq.cloudbreak.service.blueprint.ComponentLocatorService;
import com.sequenceiq.cloudbreak.service.salt.SaltStateParamsService;
import com.sequenceiq.cloudbreak.util.StackUtil;

@ExtendWith(MockitoExtension.class)
class KraftAclValidationOrchestratorServiceTest {

    private static final String BLUEPRINT_TEXT = "{}";

    private static final String BROKER_FQDN = "broker0.example.com";

    private static final String SECOND_BROKER_FQDN = "broker1.example.com";

    private static final String CHECK_KRAFT_ACLS_STATE = "kafka/check-kraft-acls";

    private static final int MAX_RETRY = 60;

    private static final int MAX_RETRY_ON_ERROR = 1;

    @Mock
    private CmTemplateProcessorFactory cmTemplateProcessorFactory;

    @Mock
    private CmTemplateProcessor cmTemplateProcessor;

    @Mock
    private ComponentLocatorService componentLocatorService;

    @Mock
    private ClusterHostServiceRunner clusterHostServiceRunner;

    @Mock
    private SaltStateParamsService saltStateParamsService;

    @Mock
    private HostOrchestrator hostOrchestrator;

    @Mock
    private StackUtil stackUtil;

    @Mock
    private StackDto stack;

    @InjectMocks
    private KraftAclValidationOrchestratorService underTest;

    @BeforeEach
    void setUp() {
        when(stack.getBlueprintJsonText()).thenReturn(BLUEPRINT_TEXT);
        when(cmTemplateProcessorFactory.get(BLUEPRINT_TEXT)).thenReturn(cmTemplateProcessor);
    }

    @Test
    void testValidateKraftAclsRunsTheStateOnEveryKafkaBrokerNodeAfterTheStatesAreRedeployed() throws CloudbreakOrchestratorFailedException {
        setUpKafkaBrokerLocations(BROKER_FQDN, SECOND_BROKER_FQDN);
        Set<Node> brokerNodes = Set.of(node(BROKER_FQDN), node(SECOND_BROKER_FQDN));
        when(stackUtil.collectReachableAndUnreachableNodes(stack, Set.of(BROKER_FQDN, SECOND_BROKER_FQDN)))
                .thenReturn(new NodeReachabilityResult(brokerNodes, Set.of()));
        OrchestratorStateParams stateParams = setUpStateParams(brokerNodes);

        underTest.validateKraftAcls(stack);

        InOrder inOrder = inOrder(clusterHostServiceRunner, hostOrchestrator);
        inOrder.verify(clusterHostServiceRunner).redeployStates(stack);
        inOrder.verify(hostOrchestrator).runOrchestratorState(stateParams);
    }

    @Test
    void testValidateKraftAclsFailsWhenAnyKafkaBrokerNodeIsUnreachable() throws CloudbreakOrchestratorFailedException {
        setUpKafkaBrokerLocations(BROKER_FQDN, SECOND_BROKER_FQDN);
        when(stackUtil.collectReachableAndUnreachableNodes(stack, Set.of(BROKER_FQDN, SECOND_BROKER_FQDN)))
                .thenReturn(new NodeReachabilityResult(Set.of(node(BROKER_FQDN)), Set.of(node(SECOND_BROKER_FQDN))));

        CloudbreakServiceException result = assertThrows(CloudbreakServiceException.class, () -> underTest.validateKraftAcls(stack));

        assertEquals("Cannot start the Zookeeper to KRaft migration because the configuration management service is not responding on these Kafka broker "
                + "nodes: [" + SECOND_BROKER_FQDN + "]", result.getMessage());
        verify(clusterHostServiceRunner, never()).redeployStates(any());
        verify(hostOrchestrator, never()).runOrchestratorState(any());
    }

    @Test
    void testValidateKraftAclsFailsWhenThereIsNoKafkaBrokerNode() throws CloudbreakOrchestratorFailedException {
        when(componentLocatorService.getComponentLocationEvenIfStopped(stack, cmTemplateProcessor, List.of(KafkaRoles.KAFKA_BROKER))).thenReturn(Map.of());

        CloudbreakServiceException result = assertThrows(CloudbreakServiceException.class, () -> underTest.validateKraftAcls(stack));

        assertEquals("Cannot validate the KRaft cluster ACLs because the cluster has no Kafka broker node.", result.getMessage());
        verify(stackUtil, never()).collectReachableAndUnreachableNodes(any(), any());
        verify(clusterHostServiceRunner, never()).redeployStates(any());
        verify(hostOrchestrator, never()).runOrchestratorState(any());
    }

    @Test
    void testValidateKraftAclsFailsWhenTheKafkaBrokerHostGroupHasNoInstance() throws CloudbreakOrchestratorFailedException {
        when(componentLocatorService.getComponentLocationEvenIfStopped(stack, cmTemplateProcessor, List.of(KafkaRoles.KAFKA_BROKER)))
                .thenReturn(Map.of(KafkaRoles.KAFKA_BROKER, List.of()));

        assertThrows(CloudbreakServiceException.class, () -> underTest.validateKraftAcls(stack));

        verify(stackUtil, never()).collectReachableAndUnreachableNodes(any(), any());
        verify(hostOrchestrator, never()).runOrchestratorState(any());
    }

    @Test
    void testValidateKraftAclsDoesNotRunTheStateWhenTheStateRedeploymentFails() throws CloudbreakOrchestratorFailedException {
        setUpKafkaBrokerLocations(BROKER_FQDN);
        when(stackUtil.collectReachableAndUnreachableNodes(stack, Set.of(BROKER_FQDN)))
                .thenReturn(new NodeReachabilityResult(Set.of(node(BROKER_FQDN)), Set.of()));
        CloudbreakServiceException redeployFailure = new CloudbreakServiceException("upload failed");
        doThrow(redeployFailure).when(clusterHostServiceRunner).redeployStates(stack);

        CloudbreakServiceException result = assertThrows(CloudbreakServiceException.class, () -> underTest.validateKraftAcls(stack));

        assertSame(redeployFailure, result);
        verify(hostOrchestrator, never()).runOrchestratorState(any());
    }

    @Test
    void testValidateKraftAclsReportsTheScriptFailureReasonOfTheNodes() throws CloudbreakOrchestratorFailedException {
        setUpKafkaBrokerLocations(BROKER_FQDN);
        Set<Node> brokerNodes = Set.of(node(BROKER_FQDN));
        when(stackUtil.collectReachableAndUnreachableNodes(stack, Set.of(BROKER_FQDN))).thenReturn(new NodeReachabilityResult(brokerNodes, Set.of()));
        OrchestratorStateParams stateParams = setUpStateParams(brokerNodes);
        Multimap<String, String> nodesWithErrors = ArrayListMultimap.create();
        nodesWithErrors.put(BROKER_FQDN, "No cluster ACL belongs to the KRaft principal");
        CloudbreakOrchestratorFailedException runnerFailure =
                new CloudbreakOrchestratorFailedException("Failed: Orchestrator component went failed in 1 min(s)", nodesWithErrors);
        doThrow(new CloudbreakOrchestratorFailedException(runnerFailure.getMessage(), runnerFailure)).when(hostOrchestrator).runOrchestratorState(stateParams);

        CloudbreakServiceException result = assertThrows(CloudbreakServiceException.class, () -> underTest.validateKraftAcls(stack));

        assertEquals("The KRaft cluster ACL validation failed on the Kafka broker nodes. " + BROKER_FQDN + ": No cluster ACL belongs to the KRaft principal",
                result.getMessage());
    }

    @Test
    void testValidateKraftAclsReportsTheOrchestratorMessageWhenThereIsNoNodeError() throws CloudbreakOrchestratorFailedException {
        setUpKafkaBrokerLocations(BROKER_FQDN);
        Set<Node> brokerNodes = Set.of(node(BROKER_FQDN));
        when(stackUtil.collectReachableAndUnreachableNodes(stack, Set.of(BROKER_FQDN))).thenReturn(new NodeReachabilityResult(brokerNodes, Set.of()));
        OrchestratorStateParams stateParams = setUpStateParams(brokerNodes);
        doThrow(new CloudbreakOrchestratorFailedException("Salt master is not available")).when(hostOrchestrator).runOrchestratorState(stateParams);

        CloudbreakServiceException result = assertThrows(CloudbreakServiceException.class, () -> underTest.validateKraftAcls(stack));

        assertEquals("The KRaft cluster ACL validation failed on the Kafka broker nodes. Salt master is not available", result.getMessage());
    }

    private void setUpKafkaBrokerLocations(String... fqdns) {
        when(componentLocatorService.getComponentLocationEvenIfStopped(stack, cmTemplateProcessor, List.of(KafkaRoles.KAFKA_BROKER)))
                .thenReturn(Map.of(KafkaRoles.KAFKA_BROKER, List.of(fqdns)));
    }

    private OrchestratorStateParams setUpStateParams(Set<Node> targetNodes) {
        OrchestratorStateParams stateParams = new OrchestratorStateParams();
        when(saltStateParamsService.createStateParamsForTargetNodes(stack, CHECK_KRAFT_ACLS_STATE, targetNodes, MAX_RETRY, MAX_RETRY_ON_ERROR))
                .thenReturn(stateParams);
        return stateParams;
    }

    private Node node(String fqdn) {
        return new Node("10.0.0.1", null, "i-1", "m5.xlarge", fqdn, "broker");
    }
}
