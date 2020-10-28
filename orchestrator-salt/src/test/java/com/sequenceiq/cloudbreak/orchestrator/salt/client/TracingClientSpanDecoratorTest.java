package com.sequenceiq.cloudbreak.orchestrator.salt.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.client.ClientRequestContext;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import com.sequenceiq.cloudbreak.orchestrator.salt.domain.Pillar;

import io.opentracing.Span;

class TracingClientSpanDecoratorTest {

    private final TracingClientSpanDecorator underTest = new TracingClientSpanDecorator();

    @Test
    void testDecoration() {
        ClientRequestContext clientRequestContext = Mockito.mock(ClientRequestContext.class);
        when(clientRequestContext.getUri()).thenReturn(URI.create("https://10.112.17.217:9443/saltboot/salt/server/pillar/distribute"));
        when(clientRequestContext.getEntity()).thenReturn(new Pillar("/cloudera-manager/database.sls", Map.of(), Set.of()));

        Span span = Mockito.mock(Span.class);
        ArgumentCaptor<String> operationNameCaptor = ArgumentCaptor.forClass(String.class);
        when(span.setOperationName(operationNameCaptor.capture())).thenReturn(span);
        ArgumentCaptor<String> tagCaptor = ArgumentCaptor.forClass(String.class);
        when(span.setTag(anyString(), tagCaptor.capture())).thenReturn(span);

        underTest.decorateRequest(clientRequestContext, span);

        assertEquals("Salt - BOOT_PILLAR_DISTRIBUTE", operationNameCaptor.getValue());
        assertEquals("/cloudera-manager/database.sls", tagCaptor.getValue());
    }

    @Test
    void testWithDifferentURI() {
        ClientRequestContext clientRequestContext = Mockito.mock(ClientRequestContext.class);
        when(clientRequestContext.getUri()).thenReturn(URI.create("https://chicken.com/saltboot/salt/server/pillar/distribute"));
        when(clientRequestContext.getEntity()).thenReturn(new Pillar("/cloudera-manager/database.sls", Map.of(), Set.of()));

        Span span = Mockito.mock(Span.class);
        ArgumentCaptor<String> operationNameCaptor = ArgumentCaptor.forClass(String.class);
        when(span.setOperationName(operationNameCaptor.capture())).thenReturn(span);
        ArgumentCaptor<String> tagCaptor = ArgumentCaptor.forClass(String.class);
        when(span.setTag(anyString(), tagCaptor.capture())).thenReturn(span);

        underTest.decorateRequest(clientRequestContext, span);

        assertEquals("Salt - BOOT_PILLAR_DISTRIBUTE", operationNameCaptor.getValue());
        assertEquals("/cloudera-manager/database.sls", tagCaptor.getValue());
    }

    @Test
    void testWithNoMatch() {
        ClientRequestContext clientRequestContext = Mockito.mock(ClientRequestContext.class);
        when(clientRequestContext.getUri()).thenReturn(URI.create("/salt1boot/salt/server/pillar/distribute"));
        Span span = Mockito.mock(Span.class);
        ArgumentCaptor<String> operationNameCaptor = ArgumentCaptor.forClass(String.class);
        when(span.setOperationName(operationNameCaptor.capture())).thenReturn(span);

        underTest.decorateRequest(clientRequestContext, span);

        assertEquals("Salt - UNKNOWN_OPERATION", operationNameCaptor.getValue());
    }
}