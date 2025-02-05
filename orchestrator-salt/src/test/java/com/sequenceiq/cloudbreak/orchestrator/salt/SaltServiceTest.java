package com.sequenceiq.cloudbreak.orchestrator.salt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;

@ExtendWith(MockitoExtension.class)
class SaltServiceTest {

    @InjectMocks
    private SaltService underTest;

    @Test
    void testWhenNonPrimaryHasNewerSaltVersion() throws CloudbreakOrchestratorFailedException {
        GatewayConfig gw1 = GatewayConfig.builder().withInstanceId("gw1").withPrimary(true).withSaltVersion("1.0").build();
        GatewayConfig gw2 = GatewayConfig.builder().withInstanceId("gw2").withPrimary(true).withSaltVersion("2.0").build();
        GatewayConfig gw3 = GatewayConfig.builder().withInstanceId("gw3").withPrimary(false).withSaltVersion("3.0").build();

        GatewayConfig result = underTest.getPrimaryGatewayConfig(List.of(gw1, gw2, gw3));
        assertEquals("3.0", result.getSaltVersion().orElse(null));
        assertEquals("gw3", result.getInstanceId());
    }

    @Test
    void testLatestSaltVersionWhenNoPrimaryHasSaltVersion() throws CloudbreakOrchestratorFailedException {
        GatewayConfig gw1 = GatewayConfig.builder().withInstanceId("gw1").withPrimary(false).withSaltVersion("1.0").build();
        GatewayConfig gw2 = GatewayConfig.builder().withInstanceId("gw2").withPrimary(false).withSaltVersion("2.0").build();
        GatewayConfig gw3 = GatewayConfig.builder().withInstanceId("gw3").withPrimary(true).build();

        GatewayConfig result = underTest.getPrimaryGatewayConfig(List.of(gw1, gw2, gw3));
        assertEquals("2.0", result.getSaltVersion().orElse(null));
        assertEquals("gw2", result.getInstanceId());
    }

    @Test
    void testPrimaryWithoutSaltWhenNoSaltVersionSpecified() throws CloudbreakOrchestratorFailedException {
        GatewayConfig gw1 = GatewayConfig.builder().withInstanceId("gw1").withPrimary(true).build();
        GatewayConfig gw2 = GatewayConfig.builder().withInstanceId("gw2").withPrimary(false).build();
        GatewayConfig gw3 = GatewayConfig.builder().withInstanceId("gw3").withPrimary(false).build();

        GatewayConfig result = underTest.getPrimaryGatewayConfig(List.of(gw1, gw2, gw3));
        assertTrue(result.isPrimary());
        assertEquals("gw1", result.getInstanceId());
    }

    @Test
    void testNoPrimaryGatewayThrowsException() {
        GatewayConfig gw1 = GatewayConfig.builder().withInstanceId("gw1").withPrimary(false).build();
        GatewayConfig gw2 = GatewayConfig.builder().withInstanceId("gw2").withPrimary(false).build();
        GatewayConfig gw3 = GatewayConfig.builder().withInstanceId("gw3").withPrimary(false).build();

        assertThrows(CloudbreakOrchestratorFailedException.class, () -> underTest.getPrimaryGatewayConfig(List.of(gw1, gw2, gw3)));
    }

}