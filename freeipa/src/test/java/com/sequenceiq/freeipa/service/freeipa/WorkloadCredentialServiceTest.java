package com.sequenceiq.freeipa.service.freeipa;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
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
import com.sequenceiq.freeipa.client.FreeIpaClient;
import com.sequenceiq.freeipa.service.freeipa.user.kerberos.KrbKeySetEncoder;
import com.sequenceiq.freeipa.service.freeipa.user.model.WorkloadCredential;

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
        FreeIpaClient freeIpaClient = mock(FreeIpaClient.class);
        Map<String, WorkloadCredential> workloadCredentialMap = Map.of(
                "user1", mock(WorkloadCredential.class),
                "user2", mock(WorkloadCredential.class),
                "user3", mock(WorkloadCredential.class),
                "user4", mock(WorkloadCredential.class));

        WorkloadCredentialService spyUnderTest = spy(underTest);
        doNothing().when(spyUnderTest).setWorkloadCredential(any(FreeIpaClient.class), anyString(), any(WorkloadCredential.class));
        BiConsumer<String, String> warnings = mock(BiConsumer.class);

        spyUnderTest.setWorkloadCredentials(freeIpaClient, workloadCredentialMap, warnings);


        verify(spyUnderTest).setWorkloadCredentials(freeIpaClient, workloadCredentialMap, warnings);
        for (Map.Entry<String, WorkloadCredential> entry : workloadCredentialMap.entrySet()) {
            verify(spyUnderTest).setWorkloadCredential(freeIpaClient, entry.getKey(), entry.getValue());
        }
        verifyNoMoreInteractions(spyUnderTest);
    }
}