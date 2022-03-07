package com.sequenceiq.cloudbreak.orchestrator.salt;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import com.sequenceiq.cloudbreak.telemetry.orchestrator.TelemetryOrchestratorModule;

public class TelemetryOrchestratorModuleTest {

    @Test
    public void testTelemetryOrchestratorModuleExistence() throws IOException {
        // GIVEN
        List<String> commonSaltModules = List.of(new ClassPathResource("salt-common/salt").getFile().list());
        List<String> saltModules = List.of(new ClassPathResource("salt/salt").getFile().list());
        // WHEN
        TelemetryOrchestratorModule[] moduleValues = TelemetryOrchestratorModule.values();
        for (TelemetryOrchestratorModule moduleVal : moduleValues) {
            assertTrue(commonSaltModules.contains(moduleVal.getValue()) || saltModules.contains(moduleVal.getValue()),
                    String.format("Salt module '%s' does not exists", moduleVal.getValue()));
        }
    }
}
