package com.sequenceiq.cloudbreak.service.stack;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.cluster.status.ExtendedHostStatuses;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.domain.stack.Component;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.service.ComponentConfigProviderService;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;
import com.sequenceiq.common.api.telemetry.model.Logging;
import com.sequenceiq.common.api.telemetry.model.Telemetry;

@ExtendWith(MockitoExtension.class)
class ServiceStatusCheckerLogLocationDecoratorTest {

    @Mock
    private ComponentConfigProviderService componentConfigProviderService;

    @Mock
    private Component component;

    @Mock
    private Json attributes;

    @Mock
    private Telemetry telemetry;

    @Mock
    private Logging logging;

    @Mock
    private InstanceMetadataView instanceMetadataView;

    @Mock
    private ExtendedHostStatuses extendedHostStatuses;

    @Mock
    private StackDto stackDto;

    @InjectMocks
    private ServiceStatusCheckerLogLocationDecorator underTest;

    @BeforeEach
    void setup() throws IOException {
        lenient().when(componentConfigProviderService.getComponent(anyLong(), any(), any())).thenReturn(component);
        lenient().when(component.getAttributes()).thenReturn(attributes);
        lenient().when(attributes.get(Telemetry.class)).thenReturn(telemetry);
        lenient().when(telemetry.getLogging()).thenReturn(logging);
        lenient().when(logging.getStorageLocation()).thenReturn("storageLocation");
        lenient().when(stackDto.getId()).thenReturn(1L);
    }

    @Test
    void testDecorateWithServiceFailure() {
        when(extendedHostStatuses.hasHostUnhealthyServices(any())).thenReturn(true);
        Map<InstanceMetadataView, Optional<String>> result = underTest.decorate(getInstanceMetadataWithReason(), extendedHostStatuses, stackDto);
        assertEquals("DefaultMessage. Please check the logs at the following location: storageLocation",
                result.entrySet().stream().findFirst().flatMap(Map.Entry::getValue).orElse(""));
        verify(componentConfigProviderService, times(1)).getComponent(anyLong(), any(), any());
    }

    @Test
    void testDecorateWithServiceFailureNullPointer() throws IOException {
        lenient().when(attributes.get(Telemetry.class)).thenReturn(null);
        when(extendedHostStatuses.hasHostUnhealthyServices(any())).thenReturn(true);
        Map<InstanceMetadataView, Optional<String>> result = underTest.decorate(getInstanceMetadataWithReason(), extendedHostStatuses, stackDto);
        assertEquals("DefaultMessage.",
                result.entrySet().stream().findFirst().flatMap(Map.Entry::getValue).orElse(""));
        verify(componentConfigProviderService, times(1)).getComponent(anyLong(), any(), any());
    }

    @Test
    void testDecorateWithServiceFailureButInstanceAlreadyInServiceFailure() {
        when(extendedHostStatuses.hasHostUnhealthyServices(any())).thenReturn(true);
        when(instanceMetadataView.getInstanceStatus()).thenReturn(InstanceStatus.SERVICES_UNHEALTHY);
        Map<InstanceMetadataView, Optional<String>> result = underTest.decorate(getInstanceMetadataWithReason(), extendedHostStatuses, stackDto);
        assertEquals("DefaultMessage.",
                result.entrySet().stream().findFirst().flatMap(Map.Entry::getValue).orElse(""));
        verify(componentConfigProviderService, times(0)).getComponent(anyLong(), any(), any());
    }

    @Test
    void testDecorateWithNoServiceFailure() {
        when(extendedHostStatuses.hasHostUnhealthyServices(any())).thenReturn(false);
        Map<InstanceMetadataView, Optional<String>> result = underTest.decorate(getInstanceMetadataWithReason(), extendedHostStatuses, stackDto);
        assertEquals("DefaultMessage.",
                result.entrySet().stream().findFirst().flatMap(Map.Entry::getValue).orElse(""));
        verify(componentConfigProviderService, times(0)).getComponent(anyLong(), any(), any());
    }

    private Map<InstanceMetadataView, Optional<String>> getInstanceMetadataWithReason() {
        return Map.of(instanceMetadataView, Optional.of("DefaultMessage."));
    }

}