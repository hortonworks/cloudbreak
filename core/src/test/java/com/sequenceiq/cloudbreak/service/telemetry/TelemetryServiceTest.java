package com.sequenceiq.cloudbreak.service.telemetry;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.isNotNull;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.HashMap;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.core.bootstrap.service.host.decorator.TelemetryDecorator;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.service.ComponentConfigProviderService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.telemetry.TelemetryComponentType;
import com.sequenceiq.common.api.telemetry.model.Telemetry;

@ExtendWith(MockitoExtension.class)
public class TelemetryServiceTest {

    private static final Long STACK_ID = 1L;

    @InjectMocks
    private TelemetryService underTest;

    @Mock
    private StackService stackService;

    @Mock
    private ComponentConfigProviderService componentConfigProviderService;

    @Mock
    private TelemetryDecorator telemetryDecorator;

    @BeforeEach
    public void setUp() {
        underTest = new TelemetryService();
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testCreateTelemetryConfigs() {
        // GIVEN
        given(stackService.getById(STACK_ID)).willReturn(stack("{}"));
        given(componentConfigProviderService.getTelemetry(STACK_ID)).willReturn(new Telemetry());
        given(telemetryDecorator.decoratePillar(anyMap(), any(), any(), any())).willReturn(new HashMap<>());
        // WHEN
        underTest.createTelemetryConfigs(STACK_ID, Set.of(TelemetryComponentType.CDP_TELEMETRY));
        // THEN
        verify(telemetryDecorator, times(1)).decoratePillar(anyMap(), any(), any(), isNotNull());
    }

    @Test
    public void testCreateTelemetryConfigsWithIOException() {
        // GIVEN
        given(stackService.getById(STACK_ID)).willReturn(stack("wrongJson"));
        given(componentConfigProviderService.getTelemetry(STACK_ID)).willReturn(new Telemetry());
        given(telemetryDecorator.decoratePillar(anyMap(), any(), any(), any())).willReturn(new HashMap<>());
        // WHEN
        underTest.createTelemetryConfigs(STACK_ID, Set.of(TelemetryComponentType.CDP_TELEMETRY));
        // THEN
        verify(telemetryDecorator, times(1)).decoratePillar(anyMap(), any(), any(), isNull());
    }

    @Test
    public void testCreateTelemetryConfigsWithEmptyJson() {
        // GIVEN
        given(stackService.getById(STACK_ID)).willReturn(stack(null));
        given(componentConfigProviderService.getTelemetry(STACK_ID)).willReturn(new Telemetry());
        given(telemetryDecorator.decoratePillar(anyMap(), any(), any(), any())).willReturn(new HashMap<>());
        // WHEN
        underTest.createTelemetryConfigs(STACK_ID, Set.of(TelemetryComponentType.CDP_TELEMETRY));
        // THEN
        verify(telemetryDecorator, times(1)).decoratePillar(anyMap(), any(), any(), isNull());
    }

    private Stack stack(String databusJson) {
        Stack stack = new Stack();
        stack.setId(STACK_ID);
        Cluster cluster = new Cluster();
        cluster.setId(STACK_ID);
        cluster.setDatabusCredential(databusJson);
        stack.setCluster(cluster);
        return stack;
    }
}
