package com.sequenceiq.freeipa.flow.freeipa.trust.setup.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.common.orchestration.Node;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.SaltConfig;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;
import com.sequenceiq.freeipa.entity.InstanceGroup;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.freeipa.trust.setup.event.FreeIpaTrustSetupUpdatePillarDataFailed;
import com.sequenceiq.freeipa.flow.freeipa.trust.setup.event.FreeIpaTrustSetupUpdatePillarDataRequest;
import com.sequenceiq.freeipa.flow.freeipa.trust.setup.event.FreeIpaTrustSetupUpdatePillarDataSuccess;
import com.sequenceiq.freeipa.service.GatewayConfigService;
import com.sequenceiq.freeipa.service.freeipa.flow.FreeIpaNodeUtilService;
import com.sequenceiq.freeipa.service.freeipa.flow.SaltConfigProvider;
import com.sequenceiq.freeipa.service.stack.StackService;

@ExtendWith(MockitoExtension.class)
class FreeIpaTrustSetupUpdatePillarDataHandlerTest {

    private static final long STACK_ID = 1L;

    @Mock
    private HostOrchestrator hostOrchestrator;

    @Mock
    private GatewayConfigService gatewayConfigService;

    @Mock
    private StackService stackService;

    @Mock
    private SaltConfigProvider saltConfigProvider;

    @Mock
    private FreeIpaNodeUtilService freeIpaNodeUtilService;

    @InjectMocks
    private FreeIpaTrustSetupUpdatePillarDataHandler underTest;

    private HandlerEvent<FreeIpaTrustSetupUpdatePillarDataRequest> handlerEvent;

    @BeforeEach
    void setUp() {
        FreeIpaTrustSetupUpdatePillarDataRequest request = new FreeIpaTrustSetupUpdatePillarDataRequest(STACK_ID);
        handlerEvent = new HandlerEvent<>(new Event<>(request));
    }

    @Test
    void testDoAccept() throws CloudbreakOrchestratorFailedException {
        Stack stack = new Stack();
        InstanceGroup instanceGroup = new InstanceGroup();
        instanceGroup.setInstanceMetaData(Set.of(new InstanceMetaData()));
        stack.setInstanceGroups(Set.of(instanceGroup));
        Set<Node> nodes = Set.of(new Node("privateIp", "publicIp", "instanceId", "instanceType", "fqdn", "hostgroup"));
        SaltConfig saltConfig = new SaltConfig();
        GatewayConfig gatewayConfig = new GatewayConfig.Builder()
                .withConnectionAddress("1.1.1.1")
                .withHostname("server")
                .withPrivateAddress("1.1.1.1")
                .withGatewayPort(80)
                .withPrimary(false)
                .build();

        when(stackService.getByIdWithListsInTransaction(STACK_ID)).thenReturn(stack);
        when(freeIpaNodeUtilService.mapInstancesToNodes(any(Set.class))).thenReturn(nodes);
        when(saltConfigProvider.getSaltConfig(stack, nodes)).thenReturn(saltConfig);
        when(gatewayConfigService.getPrimaryGatewayConfig(stack)).thenReturn(gatewayConfig);

        Selectable result = underTest.doAccept(handlerEvent);

        assertInstanceOf(FreeIpaTrustSetupUpdatePillarDataSuccess.class, result);
        verify(hostOrchestrator).saveCustomPillars(eq(saltConfig), any(), any());
    }

    @Test
    void testDoAcceptFailure() throws CloudbreakOrchestratorFailedException {
        Stack stack = new Stack();
        InstanceGroup instanceGroup = new InstanceGroup();
        instanceGroup.setInstanceMetaData(Set.of(new InstanceMetaData()));
        stack.setInstanceGroups(Set.of(instanceGroup));
        Set<Node> nodes = Set.of(new Node("privateIp", "publicIp", "instanceId", "instanceType", "fqdn", "hostgroup"));
        SaltConfig saltConfig = new SaltConfig();
        GatewayConfig gatewayConfig = new GatewayConfig.Builder()
                .withConnectionAddress("1.1.1.1")
                .withHostname("server")
                .withPrivateAddress("1.1.1.1")
                .withGatewayPort(80)
                .withPrimary(false)
                .build();
        CloudbreakOrchestratorFailedException exception = new CloudbreakOrchestratorFailedException("error");

        when(stackService.getByIdWithListsInTransaction(STACK_ID)).thenReturn(stack);
        when(freeIpaNodeUtilService.mapInstancesToNodes(any(Set.class))).thenReturn(nodes);
        when(saltConfigProvider.getSaltConfig(stack, nodes)).thenReturn(saltConfig);
        when(gatewayConfigService.getPrimaryGatewayConfig(stack)).thenReturn(gatewayConfig);
        doThrow(exception).when(hostOrchestrator).saveCustomPillars(eq(saltConfig), any(), any());

        Selectable result = underTest.doAccept(handlerEvent);

        assertInstanceOf(FreeIpaTrustSetupUpdatePillarDataFailed.class, result);
        assertEquals(exception, ((FreeIpaTrustSetupUpdatePillarDataFailed) result).getException());
    }
}
