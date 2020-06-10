package com.sequenceiq.cloudbreak.audit.converter;

import com.cloudera.thunderhead.service.audit.AuditProto;
import com.sequenceiq.cloudbreak.audit.converter.builder.ApiRequestDataBuilderProvider;
import com.sequenceiq.cloudbreak.audit.model.ApiRequestData;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ApiRequestDataBuildUpdaterTest {

    @Mock
    private ApiRequestDataBuilderProvider mockBuilderProvider;

    @Mock
    private AuditProto.ApiRequestData.Builder mockApiRequestDataBuilder;

    @Mock
    private ApiRequestData mockApiRequestData;

    @Mock
    private AuditProto.AuditEvent.Builder mockAuditEventBuilder;

    @Mock
    private AuditProto.ApiRequestData mockApiRequestDataBuilderResult;

    private ApiRequestDataBuildUpdater underTest;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        when(mockBuilderProvider.getNewApiRequestDataBuilder()).thenReturn(mockApiRequestDataBuilder);
        when(mockApiRequestDataBuilder.setMutating(anyBoolean())).thenReturn(mockApiRequestDataBuilder);
        when(mockApiRequestDataBuilder.build()).thenReturn(mockApiRequestDataBuilderResult);

        underTest = new ApiRequestDataBuildUpdater(mockBuilderProvider);
    }

    @AfterEach
    void checkAfter() {
        verify(mockApiRequestDataBuilder, times(1)).setMutating(anyBoolean());
        verify(mockApiRequestData, times(1)).getApiVersion();
        verify(mockApiRequestData, times(1)).getRequestParameters();
        verify(mockApiRequestData, times(1)).getUserAgent();
        verify(mockAuditEventBuilder, times(1)).setApiRequestData(any(AuditProto.ApiRequestData.class));
        verify(mockAuditEventBuilder, times(1)).setApiRequestData(mockApiRequestDataBuilderResult);
    }

    @Test
    void testUpdateWhenApiVersionIsNull() {
        when(mockApiRequestData.getApiVersion()).thenReturn(null);

        underTest.update(mockAuditEventBuilder, mockApiRequestData);

        verify(mockApiRequestDataBuilder, never()).setApiVersion(any());
    }

    @Test
    void testUpdateWhenApiVersionIsEmpty() {
        when(mockApiRequestData.getApiVersion()).thenReturn("");

        underTest.update(mockAuditEventBuilder, mockApiRequestData);

        verify(mockApiRequestDataBuilder, never()).setApiVersion(any());
    }

    @Test
    void testUpdateWhenApiVersionIsNotEmpty() {
        String apiVersionValue = "someApiVersion";
        when(mockApiRequestData.getApiVersion()).thenReturn(apiVersionValue);

        underTest.update(mockAuditEventBuilder, mockApiRequestData);

        verify(mockApiRequestDataBuilder, times(1)).setApiVersion(any());
        verify(mockApiRequestDataBuilder, times(1)).setApiVersion(apiVersionValue);
    }

    @Test
    void testUpdateWhenRequestParameterIsNull() {
        when(mockApiRequestData.getRequestParameters()).thenReturn(null);

        underTest.update(mockAuditEventBuilder, mockApiRequestData);

        verify(mockApiRequestDataBuilder, never()).setRequestParameters(any());
    }

    @Test
    void testUpdateWhenRequestParameterIsEmpty() {
        when(mockApiRequestData.getRequestParameters()).thenReturn("");

        underTest.update(mockAuditEventBuilder, mockApiRequestData);

        verify(mockApiRequestDataBuilder, never()).setRequestParameters(any());
    }

    @Test
    void testUpdateWhenRequestParameterIsNotEmpty() {
        String requestParams = "someRequestParamValue";
        when(mockApiRequestData.getRequestParameters()).thenReturn(requestParams);

        underTest.update(mockAuditEventBuilder, mockApiRequestData);

        verify(mockApiRequestDataBuilder, times(1)).setRequestParameters(any());
        verify(mockApiRequestDataBuilder, times(1)).setRequestParameters(requestParams);
    }

    @Test
    void testUpdateWhenUserAgentIsNull() {
        when(mockApiRequestData.getUserAgent()).thenReturn(null);

        underTest.update(mockAuditEventBuilder, mockApiRequestData);

        verify(mockApiRequestDataBuilder, never()).setUserAgent(any());
    }

    @Test
    void testUpdateWhenUserAgentIsEmpty() {
        when(mockApiRequestData.getUserAgent()).thenReturn("");

        underTest.update(mockAuditEventBuilder, mockApiRequestData);

        verify(mockApiRequestDataBuilder, never()).setUserAgent(any());
    }

    @Test
    void testUpdateWhenUserAgentIsNotEmpty() {
        String userAgentValue = "someUserAgentValue";
        when(mockApiRequestData.getUserAgent()).thenReturn(userAgentValue);

        underTest.update(mockAuditEventBuilder, mockApiRequestData);

        verify(mockApiRequestDataBuilder, times(1)).setUserAgent(any());
        verify(mockApiRequestDataBuilder, times(1)).setUserAgent(userAgentValue);
    }

}