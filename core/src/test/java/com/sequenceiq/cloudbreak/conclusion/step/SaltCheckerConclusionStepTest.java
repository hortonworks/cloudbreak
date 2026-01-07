package com.sequenceiq.cloudbreak.conclusion.step;

import static com.sequenceiq.cloudbreak.conclusion.step.ConclusionMessage.SALT_COLLECT_UNREACHABLE_FOUND;
import static com.sequenceiq.cloudbreak.conclusion.step.ConclusionMessage.SALT_COLLECT_UNREACHABLE_FOUND_DETAILS;
import static com.sequenceiq.cloudbreak.conclusion.step.ConclusionMessage.SALT_MASTER_SERVICES_UNHEALTHY;
import static com.sequenceiq.cloudbreak.conclusion.step.ConclusionMessage.SALT_MASTER_SERVICES_UNHEALTHY_DETAILS;
import static com.sequenceiq.cloudbreak.conclusion.step.ConclusionMessage.SALT_MINIONS_UNREACHABLE;
import static com.sequenceiq.cloudbreak.conclusion.step.ConclusionMessage.SALT_MINIONS_UNREACHABLE_DETAILS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.orchestration.Node;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.message.CloudbreakMessagesService;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.salt.SaltOrchestrator;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.util.NodesUnreachableException;
import com.sequenceiq.cloudbreak.util.StackUtil;

@ExtendWith(MockitoExtension.class)
public class SaltCheckerConclusionStepTest {

    @Mock
    private SaltOrchestrator saltOrchestrator;

    @Mock
    private GatewayConfigService gatewayConfigService;

    @Mock
    private StackDtoService stackDtoService;

    @Mock
    private StackUtil stackUtil;

    @InjectMocks
    private SaltCheckerConclusionStep underTest;

    @Mock
    private StackDto stackDto;

    @Mock
    private CloudbreakMessagesService cloudbreakMessagesService;

    @Test
    public void checkShouldFallbackIfOrchestratorCallFailsAndBeSuccessfulIfNoUnreachableNodeFound()
            throws NodesUnreachableException, CloudbreakOrchestratorFailedException {
        when(gatewayConfigService.getPrimaryGatewayConfig(any())).thenReturn(
                GatewayConfig.builder()
                        .withConnectionAddress("host1")
                        .withPublicAddress("1.1.1.1")
                        .withPrivateAddress("1.1.1.1")
                        .withGatewayPort(22)
                        .withInstanceId("i-1839")
                        .withKnoxGatewayEnabled(false)
                        .build()
        );
        when(stackDtoService.getById(eq(1L))).thenReturn(stackDto);
        when(saltOrchestrator.isBootstrapApiAvailable(any())).thenReturn(Boolean.TRUE);
        when(saltOrchestrator.ping(any())).thenThrow(new CloudbreakServiceException("any"));
        Set<Node> nodes = Set.of(createNode("host1"), createNode("host2"));
        when(stackUtil.collectNodes(any(), any())).thenReturn(nodes);
        when(stackUtil.collectReachableAndCheckNecessaryNodes(any(), anyCollection())).thenReturn(nodes);
        Conclusion stepResult = underTest.check(1L);

        assertFalse(stepResult.isFailureFound());
        assertNull(stepResult.getConclusion());
        assertNull(stepResult.getDetails());
        assertEquals(SaltCheckerConclusionStep.class, stepResult.getConclusionStepClass());
        verify(saltOrchestrator).ping(any());
        verify(saltOrchestrator).isBootstrapApiAvailable(any());
        verify(stackDtoService, times(1)).getById(eq(1L));
        verify(stackUtil, times(1)).collectNodes(any(), any());
        verify(stackUtil, times(1)).collectReachableAndCheckNecessaryNodes(any(), any());
    }

    @Test
    public void checkShouldFallbackAndReturnConclusionIfUnreachableNodeFound() throws NodesUnreachableException {
        when(cloudbreakMessagesService.getMessageWithArgs(eq(SALT_COLLECT_UNREACHABLE_FOUND), any())).thenReturn("collect unreachable found");
        when(cloudbreakMessagesService.getMessageWithArgs(eq(SALT_COLLECT_UNREACHABLE_FOUND_DETAILS), any())).thenReturn("collect unreachable found details");
        when(saltOrchestrator.isBootstrapApiAvailable(any())).thenReturn(Boolean.FALSE);
        when(stackDtoService.getById(eq(1L))).thenReturn(stackDto);
        when(stackUtil.collectNodes(any(), any())).thenReturn(Set.of(createNode("host1"), createNode("host2")));
        when(stackUtil.collectReachableAndCheckNecessaryNodes(any(), anyCollection())).thenThrow(new NodesUnreachableException("error", Set.of("host1")));
        Conclusion stepResult = underTest.check(1L);

        assertTrue(stepResult.isFailureFound());
        assertEquals("collect unreachable found", stepResult.getConclusion());
        assertEquals("collect unreachable found details", stepResult.getDetails());
        assertEquals(SaltCheckerConclusionStep.class, stepResult.getConclusionStepClass());
        verify(stackDtoService, times(1)).getById(eq(1L));
        verify(stackUtil, times(1)).collectNodes(any(), any());
        verify(stackUtil, times(1)).collectReachableAndCheckNecessaryNodes(any(), any());
    }

    @Test
    public void checkShouldFailAndReturnConclusionIfSaltBootIsUnavailableOnMasterFound() throws CloudbreakOrchestratorFailedException {
        when(gatewayConfigService.getPrimaryGatewayConfig(any())).thenReturn(
                GatewayConfig.builder()
                        .withConnectionAddress("host1")
                        .withPublicAddress("1.1.1.1")
                        .withPrivateAddress("1.1.1.1")
                        .withGatewayPort(22)
                        .withInstanceId("i-1839")
                        .withKnoxGatewayEnabled(false)
                        .build()
        );
        when(stackDtoService.getById(eq(1L))).thenReturn(stackDto);
        when(saltOrchestrator.isBootstrapApiAvailable(any())).thenReturn(Boolean.FALSE);
        when(cloudbreakMessagesService.getMessageWithArgs(eq(SALT_MASTER_SERVICES_UNHEALTHY), any())).thenReturn("master error");
        when(cloudbreakMessagesService.getMessageWithArgs(eq(SALT_MASTER_SERVICES_UNHEALTHY_DETAILS), any())).thenReturn("master error details");
        Conclusion stepResult = underTest.check(1L);

        assertTrue(stepResult.isFailureFound());
        assertEquals("master error", stepResult.getConclusion());
        assertEquals("master error details", stepResult.getDetails());
        assertEquals(SaltCheckerConclusionStep.class, stepResult.getConclusionStepClass());
        verify(cloudbreakMessagesService).getMessageWithArgs(eq(SALT_MASTER_SERVICES_UNHEALTHY), eq(List.of("salt-bootstrap")));
        verify(cloudbreakMessagesService).getMessageWithArgs(eq(SALT_MASTER_SERVICES_UNHEALTHY_DETAILS), eq(List.of("salt-bootstrap")));
        verify(saltOrchestrator, times(0)).ping(any());
        verify(saltOrchestrator).isBootstrapApiAvailable(any());
    }

    @Test
    public void checkShouldFailAndReturnConclusionIfUnhealthyMinionsFound() throws CloudbreakOrchestratorFailedException {
        when(gatewayConfigService.getPrimaryGatewayConfig(any())).thenReturn(
                GatewayConfig.builder()
                        .withConnectionAddress("host1")
                        .withPublicAddress("1.1.1.1")
                        .withPrivateAddress("1.1.1.1")
                        .withGatewayPort(22)
                        .withInstanceId("i-1839")
                        .withKnoxGatewayEnabled(false)
                        .build()
        );
        when(stackDtoService.getById(eq(1L))).thenReturn(stackDto);
        when(saltOrchestrator.isBootstrapApiAvailable(any())).thenReturn(Boolean.TRUE);
        when(saltOrchestrator.ping(any())).thenReturn(Map.of("host1", Boolean.TRUE, "host2", Boolean.FALSE));
        when(cloudbreakMessagesService.getMessageWithArgs(eq(SALT_MINIONS_UNREACHABLE), any())).thenReturn("minions unreachable");
        when(cloudbreakMessagesService.getMessageWithArgs(eq(SALT_MINIONS_UNREACHABLE_DETAILS), any())).thenReturn("minions unreachable details");
        Conclusion stepResult = underTest.check(1L);

        assertTrue(stepResult.isFailureFound());
        assertEquals("minions unreachable", stepResult.getConclusion());
        assertEquals("minions unreachable details", stepResult.getDetails());
        assertEquals(SaltCheckerConclusionStep.class, stepResult.getConclusionStepClass());
        verify(cloudbreakMessagesService).getMessageWithArgs(eq(SALT_MINIONS_UNREACHABLE), eq(List.of("host2")));
        verify(cloudbreakMessagesService).getMessageWithArgs(eq(SALT_MINIONS_UNREACHABLE_DETAILS), eq(List.of("host2")));
        verify(saltOrchestrator).ping(any());
        verify(saltOrchestrator).isBootstrapApiAvailable(any());
    }

    @Test
    public void checkShouldSucceedIfOnlyHealthyMinionsFound() throws CloudbreakOrchestratorFailedException {
        when(gatewayConfigService.getPrimaryGatewayConfig(any())).thenReturn(
                GatewayConfig.builder()
                        .withConnectionAddress("host1")
                        .withPublicAddress("1.1.1.1")
                        .withPrivateAddress("1.1.1.1")
                        .withGatewayPort(22)
                        .withInstanceId("i-1839")
                        .withKnoxGatewayEnabled(false)
                        .build()
        );
        when(stackDtoService.getById(eq(1L))).thenReturn(stackDto);
        when(saltOrchestrator.isBootstrapApiAvailable(any())).thenReturn(Boolean.TRUE);
        when(saltOrchestrator.ping(any())).thenReturn(Map.of("host1", Boolean.TRUE, "host2", Boolean.TRUE));
        Conclusion stepResult = underTest.check(1L);

        assertFalse(stepResult.isFailureFound());
        verify(saltOrchestrator).ping(any());
        verify(saltOrchestrator).isBootstrapApiAvailable(any());
    }

    private Node createNode(String fqdn) {
        return new Node("privateIp", "publicIp", "instanceId", "instanceType",
                fqdn, "hostGroup");
    }

}
