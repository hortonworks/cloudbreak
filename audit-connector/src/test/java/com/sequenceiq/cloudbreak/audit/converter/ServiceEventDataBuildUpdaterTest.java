package com.sequenceiq.cloudbreak.audit.converter;

import com.cloudera.thunderhead.service.audit.AuditProto;
import com.sequenceiq.cloudbreak.audit.converter.builder.ServiceEventDataBuilderProvider;
import com.sequenceiq.cloudbreak.audit.model.ServiceEventData;
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

class ServiceEventDataBuildUpdaterTest {

    @Mock
    private ServiceEventDataBuilderProvider mockBuilderProvider;

    @Mock
    private AuditProto.ServiceEventData.Builder mockServiceEventDataBuilder;

    @Mock
    private AuditProto.ServiceEventData mockAuditProtoServiceEventData;

    @Mock
    private AuditProto.AuditEvent.Builder mockAuditEventBuilder;

    @Mock
    private ServiceEventData mockServiceEventData;

    private ServiceEventDataBuildUpdater underTest;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        when(mockBuilderProvider.getNewServiceEventDataBuilder()).thenReturn(mockServiceEventDataBuilder);
        when(mockServiceEventDataBuilder.build()).thenReturn(mockAuditProtoServiceEventData);

        underTest = new ServiceEventDataBuildUpdater(mockBuilderProvider);
    }

    @AfterEach
    void checkAfter() {
        verify(mockServiceEventDataBuilder, times(1)).build();
        verify(mockServiceEventData, times(1)).getVersion();
        verify(mockServiceEventData, times(1)).getEventDetails();
        verify(mockBuilderProvider, times(1)).getNewServiceEventDataBuilder();
    }

    @Test
    void testUpdateWhenVersionIsNull() {
        when(mockServiceEventData.getVersion()).thenReturn(null);

        underTest.update(mockAuditEventBuilder, mockServiceEventData);

        verify(mockServiceEventDataBuilder, never()).setDetailsVersion(any());
    }

    @Test
    void testUpdateWhenVersionIsEmpty() {
        when(mockServiceEventData.getVersion()).thenReturn("");

        underTest.update(mockAuditEventBuilder, mockServiceEventData);

        verify(mockServiceEventDataBuilder, never()).setDetailsVersion(any());
    }

    @Test
    void testUpdateWhenVersionIsNotNull() {
        String version = "someVersion";
        when(mockServiceEventData.getVersion()).thenReturn(version);

        underTest.update(mockAuditEventBuilder, mockServiceEventData);

        verify(mockServiceEventDataBuilder, times(1)).setDetailsVersion(any());
        verify(mockServiceEventDataBuilder, times(1)).setDetailsVersion(version);
    }

    @Test
    void testUpdateWhenEventDetailIsNull() {
        when(mockServiceEventData.getEventDetails()).thenReturn(null);

        underTest.update(mockAuditEventBuilder, mockServiceEventData);

        verify(mockServiceEventDataBuilder, never()).setEventDetails(any());
    }

    @Test
    void testUpdateWhenEventDetailIsEmpty() {
        when(mockServiceEventData.getEventDetails()).thenReturn("");

        underTest.update(mockAuditEventBuilder, mockServiceEventData);

        verify(mockServiceEventDataBuilder, never()).setEventDetails(any());
    }

    @Test
    void testUpdateWhenEventDetailIsNotNull() {
        String details = "someDetail";
        when(mockServiceEventData.getEventDetails()).thenReturn(details);

        underTest.update(mockAuditEventBuilder, mockServiceEventData);

        verify(mockServiceEventDataBuilder, times(1)).setEventDetails(any());
        verify(mockServiceEventDataBuilder, times(1)).setEventDetails(details);
    }

}