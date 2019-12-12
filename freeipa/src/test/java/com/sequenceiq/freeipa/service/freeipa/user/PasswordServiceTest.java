package com.sequenceiq.freeipa.service.freeipa.user;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudera.thunderhead.service.usermanagement.UserManagementProto;
import com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient;

@ExtendWith(MockitoExtension.class)
class PasswordServiceTest {

    private static final long MOCK_TIME = 1576171731141L;

    private static final String ACCOUNT_ID = UUID.randomUUID().toString();

    private static final String ACTOR_CRN = "crn:iam:us-west-2:" + ACCOUNT_ID + ":user:" + UUID.randomUUID().toString();

    private Supplier<Long> mockTimeSupplier = () -> MOCK_TIME;

    @Mock
    private GrpcUmsClient grpcUmsClient;

    @InjectMocks
    private PasswordService underTest;

    @BeforeEach
    void setUp() {
        underTest.currentTimeSupplier = mockTimeSupplier;
    }

    @Test
    void testCalculateExpirationTimeNoPasswordPolicy() {
        UserManagementProto.Account account = UserManagementProto.Account.newBuilder().build();
        when(grpcUmsClient.getAccountDetails(eq(ACTOR_CRN), eq(ACCOUNT_ID), any())).thenReturn(account);

        assertEquals(Optional.empty(), underTest.calculateExpirationTime(ACTOR_CRN, ACCOUNT_ID));
    }

    @Test
    void testCalculateExpirationTime0PasswordPolicy() {
        UserManagementProto.Account account = UserManagementProto.Account.newBuilder()
                .setPasswordPolicy(UserManagementProto.WorkloadPasswordPolicy.newBuilder()
                        .setWorkloadPasswordMaxLifetime(0)
                        .build())
                .build();
        when(grpcUmsClient.getAccountDetails(eq(ACTOR_CRN), eq(ACCOUNT_ID), any())).thenReturn(account);

        assertEquals(Optional.empty(), underTest.calculateExpirationTime(ACTOR_CRN, ACCOUNT_ID));
    }

    @Test
    void testCalculateExpirationTimePasswordPolicy() {
        long lifetime = 18276435L;

        UserManagementProto.Account account = UserManagementProto.Account.newBuilder()
                .setPasswordPolicy(UserManagementProto.WorkloadPasswordPolicy.newBuilder()
                        .setWorkloadPasswordMaxLifetime(lifetime)
                        .build())
                .build();
        when(grpcUmsClient.getAccountDetails(eq(ACTOR_CRN), eq(ACCOUNT_ID), any())).thenReturn(account);

        assertEquals(Optional.of(Instant.ofEpochMilli(MOCK_TIME + lifetime)), underTest.calculateExpirationTime(ACTOR_CRN, ACCOUNT_ID));
    }
}