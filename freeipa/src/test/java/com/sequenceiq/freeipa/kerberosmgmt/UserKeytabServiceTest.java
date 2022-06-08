package com.sequenceiq.freeipa.kerberosmgmt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ActorKerberosKey;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetActorWorkloadCredentialsResponse;
import com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.freeipa.client.FreeIpaClient;
import com.sequenceiq.freeipa.client.FreeIpaClientException;
import com.sequenceiq.freeipa.client.model.Config;
import com.sequenceiq.freeipa.client.model.User;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.freeipa.controller.exception.UnsupportedException;
import com.sequenceiq.freeipa.kerberos.KerberosConfig;
import com.sequenceiq.freeipa.kerberos.KerberosConfigRepository;
import com.sequenceiq.freeipa.kerberosmgmt.v1.UserKeytabGenerator;
import com.sequenceiq.freeipa.kerberosmgmt.v1.UserKeytabService;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaClientFactory;

@ExtendWith(MockitoExtension.class)
class UserKeytabServiceTest {

    private static final String ACCOUNT_ID = UUID.randomUUID().toString();

    private static final String ENV_CRN = "crn:cdp:environments:us-west-1:"
            + ACCOUNT_ID + ":environment:" + UUID.randomUUID();

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:"
            + ACCOUNT_ID + ":user:" + UUID.randomUUID();

    @Mock
    private KerberosConfigRepository kerberosConfigRepository;

    @Mock
    private GrpcUmsClient grpcUmsClient;

    @Mock
    private UserKeytabGenerator userKeytabGenerator;

    @Mock
    private FreeIpaClientFactory freeIpaClientFactory;

    @InjectMocks
    private UserKeytabService underTest;

    private static List<ActorKerberosKey> newActorKerberosKeys() {
        ActorKerberosKey key1 = ActorKerberosKey.newBuilder().build();
        ActorKerberosKey key2 = ActorKerberosKey.newBuilder().build();
        return List.of(key1, key2);
    }

    private void setupKerberosConfig() {
        KerberosConfig kerberosConfig = mock(KerberosConfig.class);
        when(kerberosConfig.getRealm()).thenReturn("realm");
        when(kerberosConfigRepository.findByAccountIdAndEnvironmentCrnAndClusterNameIsNullAndArchivedIsFalse(any(), any()))
                .thenReturn(Optional.of(kerberosConfig));
    }

    private void setupGrpcResponse() {
        GetActorWorkloadCredentialsResponse response = GetActorWorkloadCredentialsResponse.newBuilder()
                .setWorkloadUsername("workloadUserName")
                .addAllKerberosKeys(newActorKerberosKeys())
                .build();
        when(grpcUmsClient.getActorWorkloadCredentials(any(), any())).thenReturn(response);
    }

    private FreeIpaClient newfreeIpaClient(boolean hasPasswordHashSuppport) throws FreeIpaClientException {
        FreeIpaClient mockFreeIpaClient = mock(FreeIpaClient.class);
        Config config = mock(Config.class);
        if (hasPasswordHashSuppport) {
            when(config.getIpauserobjectclasses()).thenReturn(Set.of("cdpUserAttr"));
        } else {
            when(config.getIpauserobjectclasses()).thenReturn(Set.of());
        }
        when(mockFreeIpaClient.getConfig()).thenReturn(config);
        return mockFreeIpaClient;
    }

    @Test
    void testGetKeytabBase64() throws FreeIpaClientException {
        String keytabBase64 = "keytabBase64...";

        setupKerberosConfig();
        setupGrpcResponse();

        FreeIpaClient freeIpaClient = newfreeIpaClient(true);
        when(freeIpaClient.userFind(any())).thenReturn(Optional.of(mock(User.class)));
        when(freeIpaClientFactory.getFreeIpaClientByAccountAndEnvironment(any(), any())).thenReturn(freeIpaClient);

        when(userKeytabGenerator.generateKeytabBase64(eq("workloadUserName"), eq("realm"), any())).thenReturn(keytabBase64);

        assertEquals(underTest.getKeytabBase64(USER_CRN, ENV_CRN), keytabBase64);
    }

    @Test
    void testGetKeytabBase64NoPasswordHashSupport() throws FreeIpaClientException {
        String keytabBase64 = "keytabBase64...";

        setupKerberosConfig();
        setupGrpcResponse();

        FreeIpaClient freeIpaClient = newfreeIpaClient(false);
        when(freeIpaClientFactory.getFreeIpaClientByAccountAndEnvironment(any(), any())).thenReturn(freeIpaClient);

        Exception exception = assertThrows(UnsupportedException.class, () -> underTest.getKeytabBase64(USER_CRN, ENV_CRN));
        assertEquals("User keytab retrieval requires a newer environment and FreeIPA version", exception.getMessage());
    }

    @Test
    void testGetKeytabBase64WorkloadUserNotInEnvironment() throws FreeIpaClientException {
        String keytabBase64 = "keytabBase64...";

        setupKerberosConfig();
        setupGrpcResponse();

        FreeIpaClient freeIpaClient = newfreeIpaClient(true);
        when(freeIpaClient.userFind(any())).thenReturn(Optional.empty());
        when(freeIpaClientFactory.getFreeIpaClientByAccountAndEnvironment(any(), any())).thenReturn(freeIpaClient);

        Exception exception = assertThrows(NotFoundException.class, () -> underTest.getKeytabBase64(USER_CRN, ENV_CRN));
        assertEquals(String.format("Workload user workloadUserName has not been synced into environment %s", ENV_CRN), exception.getMessage());
    }

    @Test
    void testGetKeytabBase64UserAndEnvironmentDifferentAccount() {
        String environementCrnDifferentAccount = "crn:cdp:environments:us-west-1:" + UUID.randomUUID() + ":environment:" + UUID.randomUUID();
        Exception exception = assertThrows(BadRequestException.class, () -> underTest.getKeytabBase64(USER_CRN, environementCrnDifferentAccount));
        assertEquals("User and Environment must be in the same account", exception.getMessage());
    }

    @Test
    void testGetKeytabBase64MissingKerberosConfig() {
        when(kerberosConfigRepository.findByAccountIdAndEnvironmentCrnAndClusterNameIsNullAndArchivedIsFalse(any(), any()))
                .thenReturn(Optional.empty());
        Exception exception =  assertThrows(NotFoundException.class, () -> underTest.getKeytabBase64(USER_CRN, ENV_CRN));
        assertEquals(String.format("KerberosConfig for environment '%s' not found.", ENV_CRN), exception.getMessage());
    }

    @Test
    void testGetKeytabBase64MissingCredentialKeys() {
        String keytabBase64 = "keytabBase64...";

        setupKerberosConfig();

        GetActorWorkloadCredentialsResponse response = GetActorWorkloadCredentialsResponse.newBuilder()
                .setWorkloadUsername("workloadUserName")
                .clearKerberosKeys()
                .build();
        when(grpcUmsClient.getActorWorkloadCredentials(any(), any())).thenReturn(response);

        Exception exception =  assertThrows(NotFoundException.class, () -> underTest.getKeytabBase64(USER_CRN, ENV_CRN));
        assertEquals("Could not retrieve workload credentials. A workload password may not have been set for this user or machine user.",
                exception.getMessage());
    }

}
