package com.sequenceiq.freeipa.service.freeipa.user;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudera.thunderhead.service.usermanagement.UserManagementProto;
import com.sequenceiq.cloudbreak.auth.crn.CrnTestUtil;
import com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient;
import com.sequenceiq.cloudbreak.common.service.Clock;

@ExtendWith(MockitoExtension.class)
class PasswordServiceTest {

    private static final long MOCK_TIME = 1576171731141L;

    private static final String ACCOUNT_ID = UUID.randomUUID().toString();

    private static final String USER_CRN = CrnTestUtil.getUserCrnBuilder()
            .setAccountId(ACCOUNT_ID)
            .setResource(UUID.randomUUID().toString())
            .build()
            .toString();

    private static final String MACHINE_USER_CRN = CrnTestUtil.getMachineUserCrnBuilder()
            .setAccountId(ACCOUNT_ID)
            .setResource(UUID.randomUUID().toString())
            .build()
            .toString();

    @Mock
    private GrpcUmsClient grpcUmsClient;

    @Mock
    private Clock clock;

    @InjectMocks
    private PasswordService underTest;

    @Test
    void testCalculateExpirationTimeForUserNoPasswordPolicies() {
        setupAccount(Optional.empty(), Optional.empty());
        assertEquals(Optional.empty(), underTest.calculateExpirationTime(USER_CRN, ACCOUNT_ID));
    }

    @Test
    void testCalculateExpirationTimeForUser0GlobalPasswordPolicy() {
        setupAccount(Optional.of(0L), Optional.empty());
        assertEquals(Optional.empty(), underTest.calculateExpirationTime(USER_CRN, ACCOUNT_ID));
    }

    @Test
    void testCalculateExpirationTimeForUserGlobalPasswordPolicy() {
        long lifetime = 18276435L;
        setupAccount(Optional.of(lifetime), Optional.empty());
        when(clock.getCurrentInstant()).thenReturn(Instant.ofEpochMilli(MOCK_TIME));

        assertEquals(Optional.of(Instant.ofEpochMilli(MOCK_TIME + lifetime)), underTest.calculateExpirationTime(USER_CRN, ACCOUNT_ID));
    }

    @Test
    void testCalculateExpirationTimeForUserMachineUserPasswordPolicy() {
        setupAccount(Optional.empty(), Optional.of(18276435L));
        assertEquals(Optional.empty(), underTest.calculateExpirationTime(USER_CRN, ACCOUNT_ID));
    }

    @Test
    void testCalculateExpirationTimeForUserGlobalAndMachineUserPasswordPolicies() {
        long globalLifetime = 18276435L;
        setupAccount(Optional.of(globalLifetime), Optional.of(globalLifetime / 2));
        when(clock.getCurrentInstant()).thenReturn(Instant.ofEpochMilli(MOCK_TIME));

        assertEquals(Optional.of(Instant.ofEpochMilli(MOCK_TIME + globalLifetime)), underTest.calculateExpirationTime(USER_CRN, ACCOUNT_ID));
    }

    @Test
    void testCalculateExpirationTimeForMachineUserNoPasswordPolicies() {
        setupAccount(Optional.empty(), Optional.empty());
        assertEquals(Optional.empty(), underTest.calculateExpirationTime(MACHINE_USER_CRN, ACCOUNT_ID));
    }

    @Test
    void testCalculateExpirationTimeForMachineUserMuPasswordPolicy() {
        long lifetime = 2592000000L;
        setupAccount(Optional.empty(), Optional.of(lifetime));
        when(clock.getCurrentInstant()).thenReturn(Instant.ofEpochMilli(MOCK_TIME));

        assertEquals(Optional.of(Instant.ofEpochMilli(MOCK_TIME + lifetime)), underTest.calculateExpirationTime(MACHINE_USER_CRN, ACCOUNT_ID));
    }

    @Test
    void testCalculateExpirationTimeForMachineUserGlobalPasswordPolicy() {
        long lifetime = 18276435L;
        setupAccount(Optional.of(lifetime), Optional.empty());
        when(clock.getCurrentInstant()).thenReturn(Instant.ofEpochMilli(MOCK_TIME));

        assertEquals(Optional.of(Instant.ofEpochMilli(MOCK_TIME + lifetime)), underTest.calculateExpirationTime(MACHINE_USER_CRN, ACCOUNT_ID));
    }

    @Test
    void testCalculateExpirationTimeForMachineUserGlobalAndMachineUserPasswordPolicies() {
        long machineUserLifetime = 2592000000L;
        setupAccount(Optional.of(machineUserLifetime / 2), Optional.of(machineUserLifetime));
        when(clock.getCurrentInstant()).thenReturn(Instant.ofEpochMilli(MOCK_TIME));

        assertEquals(Optional.of(Instant.ofEpochMilli(MOCK_TIME + machineUserLifetime)), underTest.calculateExpirationTime(MACHINE_USER_CRN, ACCOUNT_ID));
    }

    private void setupAccount(Optional<Long> globalMaxLifetime, Optional<Long> machineUserMaxLifetime) {
        UserManagementProto.Account.Builder builder = UserManagementProto.Account.newBuilder();
        globalMaxLifetime.ifPresent(lifetime -> builder.setGlobalPasswordPolicy(
                UserManagementProto.WorkloadPasswordPolicy.newBuilder()
                        .setWorkloadPasswordMaxLifetime(lifetime)
                        .build()));
        machineUserMaxLifetime.ifPresent(lifetime -> builder.setMachineUserPasswordPolicy(
                UserManagementProto.WorkloadPasswordPolicy.newBuilder()
                        .setWorkloadPasswordMaxLifetime(lifetime)
                        .build()));

        when(grpcUmsClient.getAccountDetails(eq(ACCOUNT_ID), any())).thenReturn(builder.build());
    }
}