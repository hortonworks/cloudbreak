package com.sequenceiq.cloudbreak.reactor.handler.cluster.upgrade.ccm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.cluster.ccm.upgrade.UpgradeCcmService;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ccm.UpgradeCcmRegisterClusterProxyRequest;
import com.sequenceiq.common.api.type.Tunnel;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@ExtendWith(MockitoExtension.class)
class RegisterClusterProxyHandlerTest {

    private static final Long STACK_ID = 12L;

    private static final Long CLUSTER_ID = 34L;

    @Mock
    private UpgradeCcmService upgradeCcmService;

    @Mock
    private HandlerEvent<UpgradeCcmRegisterClusterProxyRequest> event;

    @InjectMocks
    private RegisterClusterProxyHandler underTest;

    @BeforeEach
    void setUp() {
    }

    @Test
    void selector() {
        assertThat(underTest.selector()).isEqualTo("UPGRADECCMREGISTERCLUSTERPROXYREQUEST");
    }

    @Test
    void doAccept() {
        UpgradeCcmRegisterClusterProxyRequest request = new UpgradeCcmRegisterClusterProxyRequest(STACK_ID, CLUSTER_ID, Tunnel.CCM, null);
        when(event.getData()).thenReturn(request);

        Selectable result = underTest.doAccept(event);
        InOrder inOrder = inOrder(upgradeCcmService);
        inOrder.verify(upgradeCcmService).updateTunnel(STACK_ID, Tunnel.latestUpgradeTarget());
        inOrder.verify(upgradeCcmService).registerClusterProxyAndCheckHealth(STACK_ID);
        assertThat(result.selector()).isEqualTo("UPGRADECCMREGISTERCLUSTERPROXYRESULT");
    }

}
