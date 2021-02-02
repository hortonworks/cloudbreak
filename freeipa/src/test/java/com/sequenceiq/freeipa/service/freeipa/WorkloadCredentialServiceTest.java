package com.sequenceiq.freeipa.service.freeipa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudera.thunderhead.service.usermanagement.UserManagementProto;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.googlecode.jsonrpc4j.JsonRpcClientException;
import com.sequenceiq.freeipa.client.FreeIpaClient;
import com.sequenceiq.freeipa.client.FreeIpaClientException;
import com.sequenceiq.freeipa.client.model.RPCResponse;
import com.sequenceiq.freeipa.client.model.User;
import com.sequenceiq.freeipa.configuration.BatchPartitionSizeProperties;
import com.sequenceiq.freeipa.service.freeipa.user.model.WorkloadCredential;

@ExtendWith(MockitoExtension.class)
class WorkloadCredentialServiceTest {

    private static final String USER = "username";

    @Mock
    private FreeIpaClient freeIpaClient;

    @Mock
    private BatchPartitionSizeProperties batchPartitionSizeProperties;

    @InjectMocks
    private WorkloadCredentialService underTest;

    @Test
    void testSetWorkloadCredential() throws Exception {
        when(freeIpaClient.formatDate(any(Optional.class))).thenReturn(FreeIpaClient.MAX_PASSWORD_EXPIRATION_DATETIME);
        when(freeIpaClient.invoke(any(), any(), any(), any())).thenReturn(getRpcResponse());

        underTest.setWorkloadCredential(freeIpaClient, USER, getWorkloadCredential());

        verify(freeIpaClient).invoke(eq("user_mod"), eq(List.of(USER)), any(), any());
    }

    @Test
    void testSetWorkloadCredentialWhenThereAreNoModifications() throws Exception {
        when(freeIpaClient.formatDate(any(Optional.class))).thenReturn(FreeIpaClient.MAX_PASSWORD_EXPIRATION_DATETIME);
        when(freeIpaClient.invoke(any(), any(), any(), any())).thenThrow(
                new FreeIpaClientException("error", new JsonRpcClientException(4202, "", null)));

        underTest.setWorkloadCredential(freeIpaClient, USER, getWorkloadCredential());

        verify(freeIpaClient).invoke(eq("user_mod"), eq(List.of(USER)), any(), any());
    }

    @Test
    void testBatchSetWorkloadCredentials() throws Exception {
        Multimap<String, String> warnings = ArrayListMultimap.create();
        when(batchPartitionSizeProperties.getByOperation(any())).thenReturn(100);
        doNothing().when(freeIpaClient).callBatch(any(), any(), any(), any());

        underTest.setWorkloadCredentials(true, freeIpaClient, getCredentialMap(), warnings::put);

        verify(freeIpaClient).callBatch(any(), any(), any(), any());
    }

    @Test
    void testSingleSetWorkloadCredentials() throws Exception {
        Multimap<String, String> warnings = ArrayListMultimap.create();
        when(freeIpaClient.invoke(any(), any(), any(), any())).thenReturn(getRpcResponse());

        underTest.setWorkloadCredentials(false, freeIpaClient, getCredentialMap(), warnings::put);

        verify(freeIpaClient, times(4)).invoke(eq("user_mod"), any(), any(), any());
    }

    @Test
    void testSingleSetWorkloadCredentialsWhenThereAreNoModifications() throws Exception {
        Multimap<String, String> warnings = ArrayListMultimap.create();
        when(freeIpaClient.invoke(any(), any(), any(), any())).thenThrow(
                new FreeIpaClientException("error", new JsonRpcClientException(4202, "", null)));

        underTest.setWorkloadCredentials(false, freeIpaClient, getCredentialMap(), warnings::put);

        verify(freeIpaClient, times(4)).invoke(eq("user_mod"), any(), any(), any());
        assertEquals(0, warnings.size());
    }

    @Test
    void testSingleSetWorkloadCredentialsWhenErrorOccurs() throws Exception {
        Multimap<String, String> warnings = ArrayListMultimap.create();
        when(freeIpaClient.invoke(any(), any(), any(), any())).thenThrow(
                new FreeIpaClientException("error", new JsonRpcClientException(5000, "", null)));

        underTest.setWorkloadCredentials(false, freeIpaClient, getCredentialMap(), warnings::put);

        verify(freeIpaClient, times(4)).invoke(eq("user_mod"), any(), any(), any());
        assertEquals(4, warnings.size());
    }

    private Map<String, WorkloadCredential> getCredentialMap() {
        return Map.of(
                "user1", getWorkloadCredential(),
                "user2", getWorkloadCredential(),
                "user3", getWorkloadCredential(),
                "user4", getWorkloadCredential());
    }

    private WorkloadCredential getWorkloadCredential() {
        return new WorkloadCredential("hashedpassword",
                List.of(),
                Optional.of(Instant.now()),
                List.of(UserManagementProto.SshPublicKey.newBuilder().setPublicKey("fakepublickey").build(),
                        UserManagementProto.SshPublicKey.newBuilder().setPublicKey("anotherfakepublickey").build()));
    }

    private RPCResponse<Object> getRpcResponse() {
        RPCResponse<Object> response = new RPCResponse<>();
        response.setResult(new User());
        return response;
    }
}