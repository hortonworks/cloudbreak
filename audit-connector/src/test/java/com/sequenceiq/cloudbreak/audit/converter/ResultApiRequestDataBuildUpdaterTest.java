package com.sequenceiq.cloudbreak.audit.converter;

import com.cloudera.thunderhead.service.audit.AuditProto;
import com.sequenceiq.cloudbreak.audit.converter.builder.ResultApiRequestDataBuilderProvider;
import com.sequenceiq.cloudbreak.audit.model.ResultApiRequestData;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ResultApiRequestDataBuildUpdaterTest {

    @Mock
    private AuditProto.ResultApiRequestData.Builder mockResultApiRequestDataBuilder;

    @Mock
    private ResultApiRequestDataBuilderProvider builderProvider;

    @Mock
    private AuditProto.ResultApiRequestData mockResultApiRequestData;

    @Mock
    private AuditProto.AttemptAuditEventResult.Builder mockAttemptAuditEventResultBuilder;

    @Mock
    private ResultApiRequestData resultEventData;

    @Mock
    private ResultApiRequestDataBuildUpdater underTest;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);

        when(builderProvider.getNewResultApiRequestDataBuilder()).thenReturn(mockResultApiRequestDataBuilder);
        when(mockResultApiRequestDataBuilder.build()).thenReturn(mockResultApiRequestData);

        underTest = new ResultApiRequestDataBuildUpdater(builderProvider);
    }

    @AfterEach
    void checkAfter() {
        verify(builderProvider, times(1)).getNewResultApiRequestDataBuilder();
        verify(mockResultApiRequestDataBuilder, times(1)).build();
        verify(resultEventData, times(1)).getResponseParameters();
        verify(mockAttemptAuditEventResultBuilder, times(1)).setResultApiRequestData(mockResultApiRequestData);
    }

    @Test
    void testUpdateWhenResponseParametersAreEmpty() {
        when(resultEventData.getResponseParameters()).thenReturn("");

        underTest.update(mockAttemptAuditEventResultBuilder, resultEventData);

        verify(mockResultApiRequestDataBuilder, never()).setResponseParameters(anyString());
    }

    @Test
    void testUpdateWhenResponseParameterIsNull() {
        when(resultEventData.getResponseParameters()).thenReturn(null);

        underTest.update(mockAttemptAuditEventResultBuilder, resultEventData);

        verify(mockResultApiRequestDataBuilder, never()).setResponseParameters(anyString());
    }

    @Test
    void testUpdateWhenResponseParametersAreNotEmpty() {
        String parametersValue = "someValue";
        when(resultEventData.getResponseParameters()).thenReturn(parametersValue);

        underTest.update(mockAttemptAuditEventResultBuilder, resultEventData);

        verify(mockResultApiRequestDataBuilder, times(1)).setResponseParameters(anyString());
        verify(mockResultApiRequestDataBuilder, times(1)).setResponseParameters(parametersValue);
    }

}