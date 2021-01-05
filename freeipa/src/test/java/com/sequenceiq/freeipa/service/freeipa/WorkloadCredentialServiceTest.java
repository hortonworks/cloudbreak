package com.sequenceiq.freeipa.service.freeipa;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.cloudera.thunderhead.service.usermanagement.UserManagementProto;
import com.google.common.collect.ImmutableList;
import com.sequenceiq.freeipa.client.FreeIpaClient;
import com.sequenceiq.freeipa.service.freeipa.user.kerberos.KrbKeySetEncoder;
import com.sequenceiq.freeipa.service.freeipa.user.model.WorkloadCredential;

import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;

class WorkloadCredentialServiceTest {

    private WorkloadCredentialService underTest = new WorkloadCredentialService();

    @Test
    void setWorkloadCredential() throws Exception {
        FreeIpaClient freeIpaClient = mock(FreeIpaClient.class);
        String username = "username";
        WorkloadCredential workloadCredential = new WorkloadCredential("hashedpassword",
                List.of(),
                Optional.of(Instant.now()),
                List.of(UserManagementProto.SshPublicKey.newBuilder().setPublicKey("fakepublickey").build(),
                        UserManagementProto.SshPublicKey.newBuilder().setPublicKey("anotherfakepublickey").build()));

        underTest.setWorkloadCredential(freeIpaClient, username, workloadCredential);

        ArgumentCaptor<List<String>> captor = ArgumentCaptor.forClass(List.class);
        verify(freeIpaClient).userSetWorkloadCredentials(
                eq(username),
                eq(workloadCredential.getHashedPassword()),
                eq(KrbKeySetEncoder.getASNEncodedKrbPrincipalKey(workloadCredential.getKeys())),
                eq(workloadCredential.getExpirationDate()),
                captor.capture());
        assertTrue(captor.getValue().containsAll(List.of("fakepublickey", "anotherfakepublickey")));
    }

    @Test
    void setWorkloadCredentials() throws Exception {
        FreeIpaClient freeIpaClient = spy(new FreeIpaClient(null, null, null, getMockTracer()));
        doNothing().when(freeIpaClient).callBatch(any(), any());
        WorkloadCredential mockCredential = mock(WorkloadCredential.class);
        doReturn(ImmutableList.of()).when(mockCredential).getSshPublicKeys();
        Map<String, WorkloadCredential> workloadCredentialMap = Map.of(
                "user1", mockCredential,
                "user2", mockCredential,
                "user3", mockCredential,
                "user4", mockCredential);

        WorkloadCredentialService spyUnderTest = spy(underTest);
        doNothing().when(spyUnderTest).setWorkloadCredential(any(FreeIpaClient.class), anyString(), any(WorkloadCredential.class));
        BiConsumer<String, String> warnings = mock(BiConsumer.class);

        spyUnderTest.setWorkloadCredentials(freeIpaClient, workloadCredentialMap, warnings);


        verify(spyUnderTest).setWorkloadCredentials(freeIpaClient, workloadCredentialMap, warnings);
        verifyNoMoreInteractions(spyUnderTest);
    }

    private Tracer getMockTracer() {
        Tracer tracer = mock(Tracer.class);
        Span span = mock(Span.class);
        Tracer.SpanBuilder spanBuilder = mock(Tracer.SpanBuilder.class);
        SpanContext context = mock(SpanContext.class);
        lenient().when(span.context()).thenReturn(context);
        lenient().when(tracer.buildSpan(anyString())).thenReturn(spanBuilder);
        lenient().when(spanBuilder.addReference(anyString(), any())).thenReturn(spanBuilder);
        lenient().when(spanBuilder.start()).thenReturn(span);
        lenient().when(tracer.activeSpan()).thenReturn(span);
        return tracer;
    }
}