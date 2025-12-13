package com.sequenceiq.cloudbreak.structuredevent.service.audit.extractor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.of;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.audit.model.AuditEventName;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredRestCallEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.rest.RestCallDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.rest.RestRequestDetails;

@ExtendWith(MockitoExtension.class)
class RequestMethodBasedRestAuditEventNameExtractorTest {

    private RequestMethodBasedRestAuditEventNameExtractor underTest;

    @Mock
    private RestRequestDetails requestDetails;

    @Mock
    private RestCallDetails restCallDetails;

    @Mock
    private CDPStructuredRestCallEvent restCallEvent;

    @BeforeEach
    void setUp() {
        underTest = new RequestMethodBasedRestAuditEventNameExtractor();
        lenient().when(restCallEvent.getRestCall()).thenReturn(restCallDetails);
        lenient().when(restCallDetails.getRestRequest()).thenReturn(requestDetails);
    }

    @Test
    void testGetEventNameBasedOnRequestMethodWhenEventDataIsNull() {
        AuditEventName actual = underTest.getEventNameBasedOnRequestMethod(null);

        assertEquals(AuditEventName.REST_AUDIT_UNKNOWN, actual);
    }

    @Test
    void testGetEventNameBasedOnRequestMethodWhenEventDatasRestCallDetailsIsNull() {
        when(restCallEvent.getRestCall()).thenReturn(null);
        AuditEventName actual = underTest.getEventNameBasedOnRequestMethod(restCallEvent);

        assertEquals(AuditEventName.REST_AUDIT_UNKNOWN, actual);
    }

    @ParameterizedTest(name = "test method string: {0}, expected event name: {1}")
    @MethodSource("requestMethodAsStringProvider")
    void testGetEventNameBasedOnRequestMethodWhenRequestMethodIsEmptyStringOrNull(String requestMethodAsString, AuditEventName expectedEventName) {
        when(requestDetails.getMethod()).thenReturn("");

        AuditEventName actual = underTest.getEventNameBasedOnRequestMethod(restCallEvent);

        assertEquals(AuditEventName.REST_AUDIT_UNKNOWN, actual);
    }

    public static Stream<Arguments> requestMethodAsStringProvider() {
        return Stream.of(
                of(null, AuditEventName.REST_AUDIT_UNKNOWN),
                of("", AuditEventName.REST_AUDIT_UNKNOWN),
                of("GET", AuditEventName.REST_AUDIT_GET),
                of("POST", AuditEventName.REST_AUDIT_POST),
                of("PUT", AuditEventName.REST_AUDIT_PUT),
                of("DELETE", AuditEventName.REST_AUDIT_DELETE),
                of("EXTREMAL_ELEMENT_OF_REQUEST_METHOD", AuditEventName.REST_AUDIT_UNKNOWN)
        );
    }
}