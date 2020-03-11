package com.sequenceiq.freeipa.service.freeipa.user;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ActorKerberosKey;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetActorWorkloadCredentialsResponse;
import com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient;
import com.sequenceiq.freeipa.service.freeipa.user.model.WorkloadCredential;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class UmsCredentialProviderTest {

    private static final long EXPIRATION_DATE = 1584045866111L;

    private static final String PASSWORD_HASH = "passwordHash";

    private static final ActorKerberosKey ACTOR_KERBEROS_KEY_01 = ActorKerberosKey.newBuilder().build();

    private static final ActorKerberosKey ACTOR_KERBEROS_KEY_02 = ActorKerberosKey.newBuilder().build();

    private static final List<ActorKerberosKey> ACTOR_KERBEROS_KEY_LIST = List.of(ACTOR_KERBEROS_KEY_01, ACTOR_KERBEROS_KEY_02);

    @Mock
    private GrpcUmsClient grpcUmsClient;

    @InjectMocks
    private UmsCredentialProvider underTest;

    @Test
    void testGetCredentials() {
        GetActorWorkloadCredentialsResponse response = GetActorWorkloadCredentialsResponse.newBuilder()
                .setPasswordHash(PASSWORD_HASH)
                .addAllKerberosKeys(ACTOR_KERBEROS_KEY_LIST)
                .setPasswordHashExpirationDate(EXPIRATION_DATE)
                .build();
        when(grpcUmsClient.getActorWorkloadCredentials(any(), eq("user"), any())).thenReturn(response);
        WorkloadCredential credential = underTest.getCredentials("user", Optional.empty());
        assertEquals(credential.getHashedPassword(), PASSWORD_HASH);
        assertEquals(credential.getExpirationDate(), Optional.of(Instant.ofEpochMilli(EXPIRATION_DATE)));
        assertTrue(credential.getKeys().containsAll(ACTOR_KERBEROS_KEY_LIST));
    }

    @Test
    void testGetCredentialsNoExpiration() {
        GetActorWorkloadCredentialsResponse response = GetActorWorkloadCredentialsResponse.newBuilder()
                .setPasswordHash(PASSWORD_HASH)
                .addAllKerberosKeys(ACTOR_KERBEROS_KEY_LIST)
                .setPasswordHashExpirationDate(0)
                .build();
        when(grpcUmsClient.getActorWorkloadCredentials(any(), eq("user"), any())).thenReturn(response);
        WorkloadCredential credential = underTest.getCredentials("user", Optional.empty());
        assertEquals(credential.getExpirationDate(), Optional.empty());
    }
}