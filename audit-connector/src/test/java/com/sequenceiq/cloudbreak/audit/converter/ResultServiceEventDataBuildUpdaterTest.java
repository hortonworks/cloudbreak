package com.sequenceiq.cloudbreak.audit.converter;

import com.cloudera.thunderhead.service.audit.AuditProto;
import com.sequenceiq.cloudbreak.audit.converter.builder.ResultServiceEventDataBuilderProvider;
import com.sequenceiq.cloudbreak.audit.model.ResultServiceEventData;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ResultServiceEventDataBuildUpdaterTest {

    @Mock
    private ResultServiceEventDataBuilderProvider mockBuilderProvider;

    @Mock
    private AuditProto.ResultServiceEventData.Builder mockResultServiceEventDataBuilder;

    @Mock
    private AuditProto.AttemptAuditEventResult.Builder mockAttemptAuditEventResult;

    @Mock
    private ResultServiceEventData mockResultServiceEventData;

    private ResultServiceEventDataBuildUpdater underTest;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        when(mockBuilderProvider.getNewResultServiceEventDataBuilder()).thenReturn(mockResultServiceEventDataBuilder);
        when(mockResultServiceEventDataBuilder.addAllResourceCrn(any())).thenReturn(mockResultServiceEventDataBuilder);

        underTest = new ResultServiceEventDataBuildUpdater(mockBuilderProvider);
    }

    @AfterEach
    void checkAfter() {
        verify(mockResultServiceEventDataBuilder, times(1)).build();
        verify(mockResultServiceEventData, times(1)).getResultDetails();
        verify(mockResultServiceEventDataBuilder, times(1)).addAllResourceCrn(any());
    }

    @Test
    void testUpdateWhenResultDetailIsEmpty() {
        when(mockResultServiceEventData.getResultDetails()).thenReturn("");

        underTest.update(mockAttemptAuditEventResult, mockResultServiceEventData);

        verify(mockResultServiceEventDataBuilder, never()).setResultDetails(any());
    }

    @Test
    void testUpdateWhenResultDetailIsNull() {
        when(mockResultServiceEventData.getResultDetails()).thenReturn(null);

        underTest.update(mockAttemptAuditEventResult, mockResultServiceEventData);

        verify(mockResultServiceEventDataBuilder, never()).setResultDetails(any());
    }

    @Test
    void testUpdateWhenResultDetailIsNotEmpty() {
        String detailsValue = "SomeData";
        when(mockResultServiceEventData.getResultDetails()).thenReturn(detailsValue);

        underTest.update(mockAttemptAuditEventResult, mockResultServiceEventData);

        verify(mockResultServiceEventDataBuilder, times(1)).setResultDetails(any());
        verify(mockResultServiceEventDataBuilder, times(1)).setResultDetails(detailsValue);
    }

}