package com.sequenceiq.cloudbreak.service.stack;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.service.CloudbreakRuntimeException;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.RetryType;

@ExtendWith(MockitoExtension.class)
class YumLockCheckerServiceTest {

    @Mock
    private GatewayConfigService gatewayConfigService;

    @Mock
    private HostOrchestrator hostOrchestrator;

    @InjectMocks
    private YumLockCheckerService underTest;

    @Mock
    private StackDto stackDto;

    @Mock
    private GatewayConfig gatewayConfig;

    @BeforeEach
    void setup() {
        when(gatewayConfig.isPrimary()).thenReturn(true);
        when(gatewayConfigService.getAllGatewayConfigs(any())).thenReturn(List.of(gatewayConfig));
    }

    @Test
    void testYumLockedOnOneNode() throws CloudbreakOrchestratorFailedException {
        Map<String, String> saltResponse = Map.of("host1", "ok", "host2", "ok");
        when(hostOrchestrator.runCommandOnAllHosts(any(), any(), eq(RetryType.WITH_1_SEC_DELAY_MAX_3_TIMES))).thenReturn(saltResponse);
        assertDoesNotThrow(() -> underTest.validate(stackDto));
    }

    @Test
    void testYumLockedOnOneNodeYumDbLocked() throws CloudbreakOrchestratorFailedException {
        Map<String, String> saltResponse = Map.of("host1", "ok", "host2", "nok Error: rpmdb open failed nok");
        when(hostOrchestrator.runCommandOnAllHosts(any(), any(), eq(RetryType.WITH_1_SEC_DELAY_MAX_3_TIMES))).thenReturn(saltResponse);
        CloudbreakRuntimeException result = assertThrows(CloudbreakRuntimeException.class, () -> underTest.validate(stackDto));

        assertTrue(result.getMessage().contains("Operaton cannot be performed " +
                "because the yum database is locked on the following host(s): host2.\n" +
                "Please try removing the lock files and rpm databases on these machines and retry the operation"));
    }

}