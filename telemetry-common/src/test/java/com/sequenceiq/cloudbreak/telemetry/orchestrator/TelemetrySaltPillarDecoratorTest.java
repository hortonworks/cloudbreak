package com.sequenceiq.cloudbreak.telemetry.orchestrator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.BDDMockito.given;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.orchestration.OrchestratorAware;
import com.sequenceiq.cloudbreak.orchestrator.model.SaltPillarProperties;
import com.sequenceiq.cloudbreak.telemetry.TelemetryConfigView;
import com.sequenceiq.cloudbreak.telemetry.TelemetryContextProvider;
import com.sequenceiq.cloudbreak.telemetry.TelemetryPillarConfigGenerator;
import com.sequenceiq.cloudbreak.telemetry.context.TelemetryContext;

@ExtendWith(MockitoExtension.class)
public class TelemetrySaltPillarDecoratorTest {

    private TelemetrySaltPillarDecorator underTest;

    @Mock
    private TelemetryContextProvider<OrchestratorAware> telemetryContextProvider;

    @Mock
    private OrchestratorAware stack;

    @Mock
    private TelemetryContext context;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        DummyPillarConfigGenerator generator = new DummyPillarConfigGenerator();
        underTest = new TelemetrySaltPillarDecorator(telemetryContextProvider, List.of(generator));
    }

    @Test
    public void testGeneratePillarConfigMap() {
        // GIVEN
        given(telemetryContextProvider.createTelemetryContext(stack)).willReturn(context);
        // WHEN
        Map<String, SaltPillarProperties> result = underTest.generatePillarConfigMap(stack);
        // THEN
        assertTrue(result.containsKey("dummy"));
        assertTrue(result.get("dummy").getProperties().containsKey("dummy"));
        assertEquals("/dummy/init.sls", result.get("dummy").getPath());
    }

    @Test
    public void testGeneratePillarConfigMapWithoutContext() {
        // GIVEN
        given(telemetryContextProvider.createTelemetryContext(stack)).willReturn(null);
        // WHEN
        Map<String, SaltPillarProperties> result = underTest.generatePillarConfigMap(stack);
        // THEN
        assertTrue(result.isEmpty());
    }

    @Test
    public void testGeneratePillarConfigMapWithoutGenerators() {
        // GIVE
        given(telemetryContextProvider.createTelemetryContext(stack)).willReturn(context);
        underTest = new TelemetrySaltPillarDecorator(telemetryContextProvider, List.of());
        // WHEN
        Map<String, SaltPillarProperties> result = underTest.generatePillarConfigMap(stack);
        // THEN
        assertTrue(result.isEmpty());
    }

    private static class DummyPillarConfigGenerator implements TelemetryPillarConfigGenerator<DummyTelemetryView> {

        @Override
        public DummyTelemetryView createConfigs(TelemetryContext context) {
            return new DummyTelemetryView();
        }

        @Override
        public boolean isEnabled(TelemetryContext context) {
            return context != null;
        }

        @Override
        public String saltStateName() {
            return "dummy";
        }
    }

    private static class DummyTelemetryView implements TelemetryConfigView {

        @Override
        public Map<String, Object> toMap() {
            Map<String, Object> properties = new HashMap<>();
            properties.put("dummyKey1", "dummyValue1");
            properties.put("dummyKey2", "dummyValue2");
            return properties;
        }
    }
}
