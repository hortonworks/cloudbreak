package com.sequenceiq.environment.experience;

import static com.sequenceiq.cloudbreak.client.AbstractUserCrnServiceEndpoint.CRN_HEADER;
import static com.sequenceiq.cloudbreak.logger.MDCContextFilter.REQUEST_ID_HEADER;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class InvocationBuilderProviderTest {

    @Mock
    private WebTarget mockWebTarget;

    @Mock
    private Invocation.Builder mockBuilder;

    private InvocationBuilderProvider underTest;

    @BeforeEach
    void setUp() {
        underTest = new InvocationBuilderProvider();
        MockitoAnnotations.openMocks(this);
        when(mockWebTarget.request()).thenReturn(mockBuilder);
        when(mockBuilder.accept(anyString())).thenReturn(mockBuilder);
        when(mockBuilder.header(anyString(), any())).thenReturn(mockBuilder);
        when(mockBuilder.header(anyString(), anyString())).thenReturn(mockBuilder);
    }

    @Test
    void testCreateInvocationBuilderWebTargetShouldAcceptRequestCall() {
        underTest.createInvocationBuilder(mockWebTarget);

        verify(mockWebTarget, times(1)).request();
    }

    @Test
    void testCreateInvocationBuilderWebTargetShouldSetApplicationJsonAsAcceptedFormat() {
        underTest.createInvocationBuilder(mockWebTarget);

        verify(mockBuilder, times(1)).accept(anyString());
        verify(mockBuilder, times(1)).accept(APPLICATION_JSON);
    }

    @Test
    void testCreateInvocationBuilderWebTargetShouldSetCrnHeader() {
        underTest.createInvocationBuilder(mockWebTarget);

        verify(mockBuilder, times(1)).header(eq(CRN_HEADER), any());
    }

    @Test
    void testCreateInvocationBuilderWebTargetShouldSetRequestIdHeader() {
        underTest.createInvocationBuilder(mockWebTarget);

        verify(mockBuilder, times(1)).header(eq(REQUEST_ID_HEADER), anyString());
    }

}