package com.sequenceiq.cloudbreak.structuredevent.service.telemetry.log.environment;

import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPClusterStatus;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.structuredevent.event.cdp.datalake.CDPDatalakeStructuredFlowEvent;
import com.sequenceiq.cloudbreak.structuredevent.service.telemetry.converter.CDPDatalakeStructuredFlowEventToCDPDatalakeStatusChangedConverter;
import com.sequenceiq.cloudbreak.structuredevent.service.telemetry.mapper.DatalakeUseCaseMapper;
import com.sequenceiq.cloudbreak.usage.UsageReporter;

@ExtendWith(MockitoExtension.class)
class CDPDatalakeLoggerTest {

    @Mock
    private UsageReporter usageReporter;

    @Mock
    private DatalakeUseCaseMapper datalakeUseCaseMapper;

    @Mock
    private CDPDatalakeStructuredFlowEventToCDPDatalakeStatusChangedConverter datalakeStatusChangedConverter;

    @InjectMocks
    private CDPDatalakeLogger underTest;

    @Test
    void testLogWhenEventIsCDPDatalakeStructuredFlowEventAndUseCaseIsUnset() {
        CDPDatalakeStructuredFlowEvent cdpStructuredFlowEvent = new CDPDatalakeStructuredFlowEvent();
        when(datalakeUseCaseMapper.useCase(any())).thenReturn(CDPClusterStatus.Value.UNSET);
        underTest.log(cdpStructuredFlowEvent);
        verify(datalakeUseCaseMapper, times(1)).useCase(any());
        verify(datalakeStatusChangedConverter, never()).convert(any(), any());
        verify(usageReporter, never()).cdpDatalakeStatusChanged(any());
    }

    @Test
    void testLogWhenEventIsCDPDatalakeStructuredFlowEventAndUseCaseIsNotUnset() {
        CDPDatalakeStructuredFlowEvent cdpStructuredFlowEvent = new CDPDatalakeStructuredFlowEvent();
        when(datalakeUseCaseMapper.useCase(any())).thenReturn(CDPClusterStatus.Value.CREATE_STARTED);
        underTest.log(cdpStructuredFlowEvent);
        verify(datalakeUseCaseMapper, times(1)).useCase(any());
        verify(datalakeStatusChangedConverter, times(1)).convert(eq(cdpStructuredFlowEvent), eq(CDPClusterStatus.Value.CREATE_STARTED));
        verify(usageReporter, times(1)).cdpDatalakeStatusChanged(any());
    }
}