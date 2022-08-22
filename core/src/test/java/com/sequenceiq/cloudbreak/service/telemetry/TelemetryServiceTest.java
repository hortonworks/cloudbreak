package com.sequenceiq.cloudbreak.service.telemetry;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.HashMap;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.telemetry.TelemetryComponentType;
import com.sequenceiq.cloudbreak.telemetry.orchestrator.TelemetrySaltPillarDecorator;

@ExtendWith(MockitoExtension.class)
public class TelemetryServiceTest {

    private static final Long STACK_ID = 1L;

    @InjectMocks
    private TelemetryService underTest;

    @Mock
    private StackDtoService stackDtoService;

    @Mock
    private TelemetrySaltPillarDecorator telemetrySaltPillarDecorator;

    @Test
    public void testCreateTelemetryConfigs() {
        // GIVEN
        StackDto stack = mock(StackDto.class);
        given(stackDtoService.getById(STACK_ID)).willReturn(stack);
        given(telemetrySaltPillarDecorator.generatePillarConfigMap(stack)).willReturn(new HashMap<>());
        // WHEN
        underTest.createTelemetryConfigs(STACK_ID, Set.of(TelemetryComponentType.CDP_TELEMETRY));
        // THEN
        verify(stackDtoService, times(1)).getById(STACK_ID);
        verify(telemetrySaltPillarDecorator, times(1)).generatePillarConfigMap(any(StackDto.class));
    }
}
