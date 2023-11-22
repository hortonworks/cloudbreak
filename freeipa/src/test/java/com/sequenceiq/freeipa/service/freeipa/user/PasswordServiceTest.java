package com.sequenceiq.freeipa.service.freeipa.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudera.thunderhead.service.usermanagement.UserManagementProto;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.authorization.service.CommonPermissionCheckingUtils;
import com.sequenceiq.authorization.service.CustomCheckUtil;
import com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient;
import com.sequenceiq.cloudbreak.auth.crn.CrnTestUtil;
import com.sequenceiq.cloudbreak.common.dal.ResourceBasicView;
import com.sequenceiq.cloudbreak.common.service.Clock;
import com.sequenceiq.freeipa.api.v1.operation.model.OperationState;
import com.sequenceiq.freeipa.api.v1.operation.model.OperationType;
import com.sequenceiq.freeipa.entity.Operation;
import com.sequenceiq.freeipa.service.operation.OperationService;
import com.sequenceiq.freeipa.service.stack.StackService;

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

    private static final String ENVIRONMENT_CRN = "environmentCrn";

    private static final String PASSWORD = "password";

    @Mock
    private GrpcUmsClient grpcUmsClient;

    @Mock
    private Clock clock;

    @Mock
    private FreeIpaPasswordValidator freeIpaPasswordValidator;

    @Mock
    private StackService stackService;

    @Mock
    private CustomCheckUtil customCheckUtil;

    @Mock
    private CommonPermissionCheckingUtils commonPermissionCheckingUtils;

    @Mock
    private OperationService operationService;

    @Mock
    private ExecutorService usersyncExternalTaskExecutor;

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

    @Test
    void testSetPasswordWithCustomPermissionCheckShouldSetTheNewPasswordWhenTheOperationIsRunning() {
        Set<String> environmentCrns = Collections.singleton(ENVIRONMENT_CRN);
        List<ResourceBasicView> stack = createStack();
        when(stackService.findAllResourceBasicViewByEnvironmentAccountId(environmentCrns, ACCOUNT_ID)).thenReturn(stack);
        Operation expectedOperation = createOperation(OperationState.RUNNING);
        when(operationService.startOperation(ACCOUNT_ID, OperationType.SET_PASSWORD, environmentCrns, List.of(USER_CRN))).thenReturn(expectedOperation);

        AuthorizationResourceAction action = AuthorizationResourceAction.DESCRIBE_ENVIRONMENT;
        Operation actual = underTest.setPasswordWithCustomPermissionCheck(ACCOUNT_ID, USER_CRN, PASSWORD, environmentCrns, action);

        assertEquals(expectedOperation, actual);
        verify(freeIpaPasswordValidator).validate(PASSWORD);
        verify(stackService).findAllResourceBasicViewByEnvironmentAccountId(environmentCrns, ACCOUNT_ID);
    }

    @Test
    void testSetPasswordWithCustomPermissionCheckShouldSetTheNewPasswordWhenTheOperationIsRequested() {
        Set<String> environmentCrns = Collections.emptySet();
        List<ResourceBasicView> stack = createStack();
        when(stackService.findAllResourceBasicViewByAccountId(ACCOUNT_ID)).thenReturn(stack);
        Operation expectedOperation = createOperation(OperationState.REQUESTED);
        when(operationService.startOperation(ACCOUNT_ID, OperationType.SET_PASSWORD, environmentCrns, List.of(USER_CRN))).thenReturn(expectedOperation);

        AuthorizationResourceAction action = AuthorizationResourceAction.DESCRIBE_ENVIRONMENT;
        Operation actual = underTest.setPasswordWithCustomPermissionCheck(ACCOUNT_ID, USER_CRN, PASSWORD, environmentCrns, action);

        assertEquals(expectedOperation, actual);
        verify(freeIpaPasswordValidator).validate(PASSWORD);
        verify(stackService).findAllResourceBasicViewByAccountId(ACCOUNT_ID);
        verifyNoInteractions(usersyncExternalTaskExecutor);
    }

    private Operation createOperation(OperationState operationState) {
        Operation operation = new Operation();
        operation.setStatus(operationState);
        return operation;
    }

    private List<ResourceBasicView> createStack() {
        ResourceBasicView resourceBasicView = new ResourceBasicView() {
            @Override public Long getId() {
                return null;
            }

            @Override public String getResourceCrn() {
                return null;
            }

            @Override public String getName() {
                return null;
            }

            @Override public String getEnvironmentCrn() {
                return ENVIRONMENT_CRN;
            }
        };
        return Collections.singletonList(resourceBasicView);
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

        when(grpcUmsClient.getAccountDetails(eq(ACCOUNT_ID))).thenReturn(builder.build());
    }
}