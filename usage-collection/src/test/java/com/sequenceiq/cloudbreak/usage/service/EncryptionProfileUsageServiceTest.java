package com.sequenceiq.cloudbreak.usage.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.sequenceiq.cloudbreak.usage.UsageReporter;

@ExtendWith(MockitoExtension.class)
class EncryptionProfileUsageServiceTest {

    private static final String CRN = "crn:cdp:environments:us-west-1:accountId:encryptionProfile:uuid";

    private static final String ACCOUNT_ID = "accountId";

    @Mock
    private UsageReporter usageReporter;

    @InjectMocks
    private EncryptionProfileUsageService underTest;

    @Captor
    private ArgumentCaptor<UsageProto.CDPEncryptionProfileEvent> eventCaptor;

    @Test
    void testCreatedUsageReport() {
        underTest.sendCreatedUsageReport(CRN, ACCOUNT_ID, Set.of("TLSv1.3", "TLSv1.2"),
                List.of("TLS_AES_256_GCM_SHA384", "TLS_AES_128_GCM_SHA256"));

        verify(usageReporter).cdpEncryptionProfileEvent(eventCaptor.capture());
        UsageProto.CDPEncryptionProfileEvent event = eventCaptor.getValue();
        assertEquals(UsageProto.CDPEncryptionProfileStatus.Value.CREATED, event.getStatus());
        assertEquals(CRN, event.getEncryptionProfileCrn());
        assertEquals(ACCOUNT_ID, event.getAccountId());
        assertEquals("TLSv1.2,TLSv1.3", event.getTlsVersions());
        assertEquals("TLS_AES_256_GCM_SHA384,TLS_AES_128_GCM_SHA256", event.getCipherSuites());
    }

    @Test
    void testDeletedUsageReport() {
        underTest.sendDeletedUsageReport(CRN, ACCOUNT_ID, List.of("TLSv1.2"), List.of("TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256"));

        verify(usageReporter).cdpEncryptionProfileEvent(eventCaptor.capture());
        UsageProto.CDPEncryptionProfileEvent event = eventCaptor.getValue();
        assertEquals(UsageProto.CDPEncryptionProfileStatus.Value.DELETED, event.getStatus());
        assertEquals(CRN, event.getEncryptionProfileCrn());
        assertEquals("TLSv1.2", event.getTlsVersions());
        assertEquals("TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256", event.getCipherSuites());
    }

    @Test
    void testUsageReportWithNullValues() {
        underTest.sendCreatedUsageReport(null, null, null, Arrays.asList("TLS_AES_128_GCM_SHA256", null));

        verify(usageReporter).cdpEncryptionProfileEvent(eventCaptor.capture());
        UsageProto.CDPEncryptionProfileEvent event = eventCaptor.getValue();
        assertEquals("", event.getEncryptionProfileCrn());
        assertEquals("", event.getAccountId());
        assertEquals("", event.getTlsVersions());
        assertEquals("TLS_AES_128_GCM_SHA256", event.getCipherSuites());
    }

    @Test
    void testUsageReportFailureIsNotPropagated() {
        doThrow(new RuntimeException("error")).when(usageReporter).cdpEncryptionProfileEvent(any());

        underTest.sendDeletedUsageReport(CRN, ACCOUNT_ID, List.of(), List.of());

        verify(usageReporter).cdpEncryptionProfileEvent(any());
    }
}
