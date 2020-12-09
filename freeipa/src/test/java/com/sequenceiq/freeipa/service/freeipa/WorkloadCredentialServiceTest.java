package com.sequenceiq.freeipa.service.freeipa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatcher;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.googlecode.jsonrpc4j.JsonRpcClientException;
import com.sequenceiq.cloudbreak.auth.crn.CrnTestUtil;
import com.sequenceiq.cloudbreak.client.RPCResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.WorkloadCredentialsUpdateType;
import com.sequenceiq.freeipa.client.FreeIpaClient;
import com.sequenceiq.freeipa.client.FreeIpaClientException;
import com.sequenceiq.freeipa.client.model.User;
import com.sequenceiq.freeipa.configuration.BatchPartitionSizeProperties;
import com.sequenceiq.freeipa.service.freeipa.user.UserSyncTestUtils;
import com.sequenceiq.freeipa.service.freeipa.user.conversion.UserMetadataConverter;
import com.sequenceiq.freeipa.service.freeipa.user.model.UserSyncOptions;
import com.sequenceiq.freeipa.service.freeipa.user.model.WorkloadCredential;
import com.sequenceiq.freeipa.service.freeipa.user.model.WorkloadCredentialUpdate;

@ExtendWith(MockitoExtension.class)
class WorkloadCredentialServiceTest {

    private static final String USER = "username";

    private static final String USER_CRN = getRandomUserCrn();

    private static final List<String> USERS = List.of(
            "user1",
            "user2",
            "user3",
            "user4");

    private static final long UMS_WORKLOAD_CREDENTIALS_VERSION = 123L;

    @Mock
    private FreeIpaClient freeIpaClient;

    @Mock
    private BatchPartitionSizeProperties batchPartitionSizeProperties;

    @Mock
    private UserMetadataConverter userMetadataConverter;

    @InjectMocks
    private WorkloadCredentialService underTest;

    @Test
    void testSetWorkloadCredential() throws Exception {
        when(freeIpaClient.formatDate(any(Optional.class))).thenReturn(FreeIpaClient.MAX_PASSWORD_EXPIRATION_DATETIME);
        when(freeIpaClient.invoke(any(), any(), any(), any())).thenReturn(getRpcResponse());

        underTest.setWorkloadCredential(false, freeIpaClient, new WorkloadCredentialUpdate(USER, USER_CRN, createWorkloadCredential()));

        verify(freeIpaClient).invoke(eq("user_mod"), eq(List.of(USER)), any(), any());
    }

    @Test
    void testSetWorkloadCredentialWhenThereAreNoModifications() throws Exception {
        when(freeIpaClient.formatDate(any(Optional.class))).thenReturn(FreeIpaClient.MAX_PASSWORD_EXPIRATION_DATETIME);
        when(freeIpaClient.invoke(any(), any(), any(), any())).thenThrow(
                new FreeIpaClientException("error", new JsonRpcClientException(4202, "", null)));

        underTest.setWorkloadCredential(false, freeIpaClient, new WorkloadCredentialUpdate(USER, USER_CRN, createWorkloadCredential()));

        verify(freeIpaClient).invoke(eq("user_mod"), eq(List.of(USER)), any(), any());
    }

    @Test
    void testSetWorkloadCredentialWithUpdateOptimization() throws Exception {
        doReturn("userMetadataJson").when(userMetadataConverter).toUserMetadataJson(USER_CRN, UMS_WORKLOAD_CREDENTIALS_VERSION);
        when(freeIpaClient.invoke(any(), any(), any(), any())).thenReturn(getRpcResponse());

        underTest.setWorkloadCredential(true, freeIpaClient, new WorkloadCredentialUpdate(USER, USER_CRN, createWorkloadCredential()));

        verify(freeIpaClient).invoke(eq("user_mod"), eq(List.of(USER)), argThat(matchesTitleAttribute("userMetadataJson")), any());
    }

    @Test
    void testBatchSetWorkloadCredentials() throws Exception {
        Multimap<String, String> warnings = ArrayListMultimap.create();
        when(batchPartitionSizeProperties.getByOperation(any())).thenReturn(100);
        doNothing().when(freeIpaClient).callBatch(any(), any(), any(), any());

        setWorkloadCredentials(true, false, freeIpaClient, getCredentialMap(), getUsersWithCredentialsToUpdate(), getUserToCrnMap(), warnings::put);

        verify(freeIpaClient).callBatch(any(), any(), any(), any());
    }

    @Test
    void testBatchSetWorkloadCredentialsWithUpdateOptimization() throws Exception {
        Multimap<String, String> warnings = ArrayListMultimap.create();
        when(batchPartitionSizeProperties.getByOperation(any())).thenReturn(100);
        doNothing().when(freeIpaClient).callBatch(any(), any(), any(), any());
        when(userMetadataConverter.toUserMetadataJson(any(), anyLong())).thenReturn("userMetadataJson");

        setWorkloadCredentials(true, true, freeIpaClient, getCredentialMap(), getUsersWithCredentialsToUpdate(), getUserToCrnMap(), warnings::put);

        verify(freeIpaClient).callBatch(any(), any(), any(), any());
        verify(userMetadataConverter, times(USERS.size())).toUserMetadataJson(any(), anyLong());
    }

    @Test
    void testSingleSetWorkloadCredentials() throws Exception {
        Multimap<String, String> warnings = ArrayListMultimap.create();
        when(freeIpaClient.invoke(any(), any(), any(), any())).thenReturn(getRpcResponse());

        setWorkloadCredentials(false, false, freeIpaClient, getCredentialMap(), getUsersWithCredentialsToUpdate(), getUserToCrnMap(), warnings::put);

        verify(freeIpaClient, times(4)).invoke(eq("user_mod"), any(), any(), any());
    }

    @Test
    void testSingleSetWorkloadCredentialsWithUpdateOptimization() throws Exception {
        Multimap<String, String> warnings = ArrayListMultimap.create();
        when(freeIpaClient.invoke(any(), any(), any(), any())).thenReturn(getRpcResponse());
        Map<String, String> userToCrnMap = getUserToCrnMap();
        userToCrnMap.forEach((username, crn) ->
            when(userMetadataConverter.toUserMetadataJson(eq(crn), anyLong())).thenReturn(username + "-userMetadataJson"));

        setWorkloadCredentials(false, true, freeIpaClient, getCredentialMap(), getUsersWithCredentialsToUpdate(), userToCrnMap, warnings::put);

        for (String username : userToCrnMap.keySet()) {
            verify(freeIpaClient).invoke(eq("user_mod"), eq(List.of(username)), argThat(matchesTitleAttribute(username + "-userMetadataJson")), any());
        }
    }

    @Test
    void testSingleSetWorkloadCredentialsWhenThereAreNoModifications() throws Exception {
        Multimap<String, String> warnings = ArrayListMultimap.create();
        when(freeIpaClient.invoke(any(), any(), any(), any())).thenThrow(
                new FreeIpaClientException("error", new JsonRpcClientException(4202, "", null)));

        setWorkloadCredentials(false, false, freeIpaClient, getCredentialMap(), getUsersWithCredentialsToUpdate(), getUserToCrnMap(), warnings::put);

        verify(freeIpaClient, times(4)).invoke(eq("user_mod"), any(), any(), any());
        assertEquals(0, warnings.size());
    }

    @Test
    void testSingleSetWorkloadCredentialsWhenErrorOccurs() throws Exception {
        Multimap<String, String> warnings = ArrayListMultimap.create();
        when(freeIpaClient.invoke(any(), any(), any(), any())).thenThrow(
                new FreeIpaClientException("error", new JsonRpcClientException(5000, "", null)));

        setWorkloadCredentials(false, false, freeIpaClient, getCredentialMap(), getUsersWithCredentialsToUpdate(), getUserToCrnMap(), warnings::put);

        verify(freeIpaClient, times(4)).invoke(eq("user_mod"), any(), any(), any());
        assertEquals(4, warnings.size());
    }

    private Map<String, WorkloadCredential> getCredentialMap() {
        return USERS.stream().collect(Collectors.toMap(Function.identity(), username -> createWorkloadCredential()));
    }

    private WorkloadCredential createWorkloadCredential() {
        return UserSyncTestUtils.createWorkloadCredential("hashedPassword", UMS_WORKLOAD_CREDENTIALS_VERSION);
    }

    private Set<String> getUsersWithCredentialsToUpdate() {
        return Set.copyOf(USERS);
    }

    private Map<String, String> getUserToCrnMap() {
        return USERS.stream().collect(Collectors.toMap(Function.identity(), username -> getRandomUserCrn()));
    }

    private RPCResponse<Object> getRpcResponse() {
        RPCResponse<Object> response = new RPCResponse<>();
        response.setResult(new User());
        return response;
    }

    private ArgumentMatcher<Map<String, Object>> matchesTitleAttribute(String expectedTitle) {
        String titleSetattrArg = String.format("title=%s", expectedTitle);
        // The downcast may fail if the params are not as expected, but that's OK because it will cause the test to fail, as it should.
        return params -> params.containsKey("setattr") && ((List<String>) params.get("setattr")).contains(titleSetattrArg);
    }

    private static String getRandomUserCrn() {
        return CrnTestUtil.getUserCrnBuilder()
                .setAccountId(UUID.randomUUID().toString())
                .setResource(UUID.randomUUID().toString())
                .build().toString();
    }

    private void setWorkloadCredentials(boolean batchCallEnabled, boolean updateOptimizationEnabled, FreeIpaClient ipaClient,
            Map<String, WorkloadCredential> usersWorkloadCredentialMap, Set<String> usersWithCredentialsToUpdate, Map<String, String> userToCrnMap,
            BiConsumer<String, String> warnings) throws FreeIpaClientException {
        ImmutableSet<WorkloadCredentialUpdate> credentialUpdates = usersWithCredentialsToUpdate.stream()
                .map(username -> new WorkloadCredentialUpdate(username, userToCrnMap.get(username), usersWorkloadCredentialMap.get(username)))
                .collect(ImmutableSet.toImmutableSet());
        WorkloadCredentialsUpdateType credentialsUpdateType = updateOptimizationEnabled ?
                WorkloadCredentialsUpdateType.UPDATE_IF_CHANGED : WorkloadCredentialsUpdateType.FORCE_UPDATE;
        underTest.setWorkloadCredentials(new UserSyncOptions(batchCallEnabled, credentialsUpdateType), ipaClient, credentialUpdates,
                warnings);
    }
}