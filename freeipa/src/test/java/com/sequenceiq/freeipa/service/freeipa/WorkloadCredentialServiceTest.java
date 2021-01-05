package com.sequenceiq.freeipa.service.freeipa;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudera.thunderhead.service.usermanagement.UserManagementProto;
import com.google.common.collect.ImmutableList;
import com.sequenceiq.freeipa.configuration.BatchPartitionSizeProperties;
import com.sequenceiq.freeipa.client.FreeIpaClient;
import com.sequenceiq.freeipa.client.model.RPCResponse;
import com.sequenceiq.freeipa.client.model.User;
import com.sequenceiq.freeipa.service.freeipa.user.model.WorkloadCredential;

@ExtendWith(MockitoExtension.class)
class WorkloadCredentialServiceTest {

    @Mock
    private FreeIpaClient freeIpaClient;

    @Mock
    private BatchPartitionSizeProperties batchPartitionSizeProperties;

    @InjectMocks
    private WorkloadCredentialService underTest;

    @Test
    void setWorkloadCredential() throws Exception {
        when(freeIpaClient.formatDate(any(Optional.class))).thenReturn(FreeIpaClient.MAX_PASSWORD_EXPIRATION_DATETIME);
        RPCResponse<Object> response = new RPCResponse<>();
        response.setResult(new User());
        when(freeIpaClient.invoke(any(), any(), any(), any())).thenReturn(response);
        String username = "username";
        WorkloadCredential workloadCredential = new WorkloadCredential("hashedpassword",
                List.of(),
                Optional.of(Instant.now()),
                List.of(UserManagementProto.SshPublicKey.newBuilder().setPublicKey("fakepublickey").build(),
                        UserManagementProto.SshPublicKey.newBuilder().setPublicKey("anotherfakepublickey").build()));

        underTest.setWorkloadCredential(freeIpaClient, username, workloadCredential);

        verify(freeIpaClient).invoke(eq("user_mod"), eq(List.of(username)), any(), any());
    }

    @Test
    void setWorkloadCredentials() throws Exception {
        when(batchPartitionSizeProperties.getByOperation(any())).thenReturn(100);
        doNothing().when(freeIpaClient).callBatch(any(), any(), any(), any());
        WorkloadCredential mockCredential = mock(WorkloadCredential.class);
        doReturn(ImmutableList.of()).when(mockCredential).getSshPublicKeys();
        Map<String, WorkloadCredential> workloadCredentialMap = Map.of(
                "user1", mockCredential,
                "user2", mockCredential,
                "user3", mockCredential,
                "user4", mockCredential);

        WorkloadCredentialService spyUnderTest = spy(underTest);
        BiConsumer<String, String> warnings = mock(BiConsumer.class);

        spyUnderTest.setWorkloadCredentials(true, freeIpaClient, workloadCredentialMap, warnings);

        verify(spyUnderTest).setWorkloadCredentials(true, freeIpaClient, workloadCredentialMap, warnings);
        verifyNoMoreInteractions(spyUnderTest);
    }
}