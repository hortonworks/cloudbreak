package com.sequenceiq.freeipa.controller;

import static com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider.INTERNAL_ACTOR_CRN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SetPasswordRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SyncOperationStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SynchronizationStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SynchronizeAllUsersRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SynchronizeUserRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.WorkloadCredentialsUpdateType;
import com.sequenceiq.freeipa.controller.exception.SyncOperationAlreadyRunningException;
import com.sequenceiq.freeipa.converter.freeipa.user.OperationToSyncOperationStatus;
import com.sequenceiq.freeipa.entity.Operation;
import com.sequenceiq.freeipa.service.freeipa.user.PasswordService;
import com.sequenceiq.freeipa.service.freeipa.user.UserSyncService;
import com.sequenceiq.freeipa.service.operation.OperationService;

@ExtendWith(MockitoExtension.class)
public class UserV1ControllerTest {

    private static final String ACCOUNT_ID = UUID.randomUUID().toString();

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:" + ACCOUNT_ID + ":user:" + UUID.randomUUID().toString();

    private static final String MACHINE_USER_CRN = "crn:cdp:iam:us-west-1:" + ACCOUNT_ID + ":machineUser:" + UUID.randomUUID().toString();

    private static final String ENV_CRN = "crn:cdp:environment:us-west-1:" + ACCOUNT_ID + ":environment:" + UUID.randomUUID().toString();

    @InjectMocks
    private UserV1Controller underTest;

    @Mock
    private UserSyncService userSyncService;

    @Mock
    private PasswordService passwordService;

    @Mock
    private OperationService operationService;

    @Mock
    private OperationToSyncOperationStatus operationToSyncOperationStatus;

    @Test
    void synchronizeUser() {
        Operation operation = mock(Operation.class);
        when(userSyncService.synchronizeUsersWithCustomPermissionCheck(any(), any(), any(), any(), any())).thenReturn(operation);
        SyncOperationStatus status = mock(SyncOperationStatus.class);
        when(operationToSyncOperationStatus.convert(operation)).thenReturn(status);

        SynchronizeUserRequest request = mock(SynchronizeUserRequest.class);

        assertEquals(status, ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.synchronizeUser(request)));

        verify(userSyncService, times(1)).synchronizeUsersWithCustomPermissionCheck(ACCOUNT_ID, USER_CRN, Set.of(),
                WorkloadCredentialsUpdateType.UPDATE_IF_CHANGED, AuthorizationResourceAction.DESCRIBE_ENVIRONMENT);
    }

    @Test
    void synchronizeUserMachineUser() {
        Operation operation = mock(Operation.class);
        when(userSyncService.synchronizeUsersWithCustomPermissionCheck(any(), any(), any(), any(), any())).thenReturn(operation);
        SyncOperationStatus status = mock(SyncOperationStatus.class);
        when(operationToSyncOperationStatus.convert(operation)).thenReturn(status);

        SynchronizeUserRequest request = mock(SynchronizeUserRequest.class);

        assertEquals(status, ThreadBasedUserCrnProvider.doAs(MACHINE_USER_CRN, () -> underTest.synchronizeUser(request)));

        verify(userSyncService, times(1)).synchronizeUsersWithCustomPermissionCheck(ACCOUNT_ID, MACHINE_USER_CRN,
                Set.of(),  WorkloadCredentialsUpdateType.UPDATE_IF_CHANGED, AuthorizationResourceAction.DESCRIBE_ENVIRONMENT);
    }

    @Test
    void synchronizeUserRejected() {
        Operation operation = mock(Operation.class);
        when(userSyncService.synchronizeUsersWithCustomPermissionCheck(ACCOUNT_ID, USER_CRN, Set.of(),
                WorkloadCredentialsUpdateType.UPDATE_IF_CHANGED, AuthorizationResourceAction.DESCRIBE_ENVIRONMENT)).thenReturn(operation);
        SyncOperationStatus status = mock(SyncOperationStatus.class);
        when(status.getStatus()).thenReturn(SynchronizationStatus.REJECTED);
        when(operationToSyncOperationStatus.convert(operation)).thenReturn(status);

        SynchronizeUserRequest request = mock(SynchronizeUserRequest.class);

        assertThrows(SyncOperationAlreadyRunningException.class, () -> ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.synchronizeUser(request)));
    }

    @Test
    void synchronizeAllUsers() {
        Set<String> environments = Set.of(ENV_CRN);
        SynchronizeAllUsersRequest request = new SynchronizeAllUsersRequest();
        request.setEnvironments(environments);

        Operation operation = mock(Operation.class);
        when(userSyncService.synchronizeUsersWithCustomPermissionCheck(any(), any(), any(), any(), any())).thenReturn(operation);
        SyncOperationStatus status = mock(SyncOperationStatus.class);
        when(operationToSyncOperationStatus.convert(operation)).thenReturn(status);

        assertEquals(status, ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.synchronizeAllUsers(request)));

        verify(userSyncService, times(1)).synchronizeUsersWithCustomPermissionCheck(ACCOUNT_ID, USER_CRN, environments,
                WorkloadCredentialsUpdateType.UPDATE_IF_CHANGED, AuthorizationResourceAction.DESCRIBE_ENVIRONMENT);
    }

    @Test
    void synchronizeAllUsersDeleteWorkloadUser() {
        SynchronizeAllUsersRequest request = new SynchronizeAllUsersRequest();
        request.setEnvironments(Set.of());

        Operation operation = mock(Operation.class);
        when(userSyncService.synchronizeUsersWithCustomPermissionCheck(any(), any(), any(), any(), any())).thenReturn(operation);
        SyncOperationStatus status = mock(SyncOperationStatus.class);
        when(operationToSyncOperationStatus.convert(operation)).thenReturn(status);

        assertEquals(status, ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.synchronizeAllUsers(request)));

        verify(userSyncService, times(1)).synchronizeUsersWithCustomPermissionCheck(ACCOUNT_ID, USER_CRN, Set.of(),
                WorkloadCredentialsUpdateType.UPDATE_IF_CHANGED, AuthorizationResourceAction.DESCRIBE_ENVIRONMENT);
    }

    @Test
    void synchronizeAllUsersAsInternalActor() {
        Set<String> environments = Set.of(ENV_CRN);
        SynchronizeAllUsersRequest request = new SynchronizeAllUsersRequest();
        request.setEnvironments(environments);
        request.setAccountId(ACCOUNT_ID);

        Operation operation = mock(Operation.class);
        when(userSyncService.synchronizeUsersWithCustomPermissionCheck(any(), any(), any(), any(), any())).thenReturn(operation);
        SyncOperationStatus status = mock(SyncOperationStatus.class);
        when(operationToSyncOperationStatus.convert(operation)).thenReturn(status);

        assertEquals(status, ThreadBasedUserCrnProvider.doAsInternalActor(() -> underTest.synchronizeAllUsers(request)));

        verify(userSyncService, times(1)).synchronizeUsersWithCustomPermissionCheck(ACCOUNT_ID, INTERNAL_ACTOR_CRN,
                environments, WorkloadCredentialsUpdateType.UPDATE_IF_CHANGED, AuthorizationResourceAction.DESCRIBE_ENVIRONMENT);
    }

    @Test
    void synchronizeAllUsersUnauthorizedAccountId() {
        SynchronizeAllUsersRequest request = new SynchronizeAllUsersRequest();
        request.setAccountId(ACCOUNT_ID);
        String actorInDifferentAccount = "crn:cdp:iam:us-west-1:" + UUID.randomUUID() + ":user:" + UUID.randomUUID();
        assertThrows(AccessDeniedException.class, () -> ThreadBasedUserCrnProvider.doAs(actorInDifferentAccount, () -> underTest.synchronizeAllUsers(request)));
    }

    @Test
    void synchronizeAllUsersRejected() {
        Set<String> environments = Set.of(ENV_CRN);
        SynchronizeAllUsersRequest request = new SynchronizeAllUsersRequest();
        request.setEnvironments(environments);

        Operation operation = mock(Operation.class);
        when(userSyncService.synchronizeUsersWithCustomPermissionCheck(ACCOUNT_ID, USER_CRN, environments,
                WorkloadCredentialsUpdateType.UPDATE_IF_CHANGED, AuthorizationResourceAction.DESCRIBE_ENVIRONMENT)).thenReturn(operation);
        SyncOperationStatus status = mock(SyncOperationStatus.class);
        when(status.getStatus()).thenReturn(SynchronizationStatus.REJECTED);
        when(operationToSyncOperationStatus.convert(operation)).thenReturn(status);

        assertThrows(SyncOperationAlreadyRunningException.class,
                () -> ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.synchronizeAllUsers(request)));
    }

    @Test
    void getStatus() {
        String operationId = "testId";

        Operation operation = mock(Operation.class);
        when(operationService.getOperationForAccountIdAndOperationId(ACCOUNT_ID, operationId)).thenReturn(operation);
        SyncOperationStatus status = mock(SyncOperationStatus.class);
        when(operationToSyncOperationStatus.convert(operation)).thenReturn(status);

        assertEquals(status, ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.getSyncOperationStatus(operationId)));

        verify(operationService, times(1)).getOperationForAccountIdAndOperationId(ACCOUNT_ID, operationId);
    }

    @Test
    void getStatusInternal() {
        String operationId = "testId";

        Operation operation = mock(Operation.class);
        when(operationService.getOperationForAccountIdAndOperationId(ACCOUNT_ID, operationId)).thenReturn(operation);
        SyncOperationStatus status = mock(SyncOperationStatus.class);
        when(operationToSyncOperationStatus.convert(operation)).thenReturn(status);

        assertEquals(status, ThreadBasedUserCrnProvider.doAsInternalActor(() ->
                underTest.getSyncOperationStatusInternal(ACCOUNT_ID, operationId)));

        verify(operationService, times(1)).getOperationForAccountIdAndOperationId(ACCOUNT_ID, operationId);
    }

    @Test
    void setPassword() {
        String password = "password";
        SetPasswordRequest request = mock(SetPasswordRequest.class);
        when(request.getPassword()).thenReturn(password);

        Operation operation = mock(Operation.class);
        when(passwordService.setPasswordWithCustomPermissionCheck(any(), any(), any(), any(), any())).thenReturn(operation);
        SyncOperationStatus status = mock(SyncOperationStatus.class);
        when(operationToSyncOperationStatus.convert(operation)).thenReturn(status);

        assertEquals(status, ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.setPassword(request)));

        verify(passwordService, times(1)).setPasswordWithCustomPermissionCheck(ACCOUNT_ID, USER_CRN,
                password, new HashSet<>(), AuthorizationResourceAction.DESCRIBE_ENVIRONMENT);
    }

    @Test
    void setPasswordRejected() {
        String password = "password";
        SetPasswordRequest request = mock(SetPasswordRequest.class);
        when(request.getPassword()).thenReturn(password);

        Operation operation = mock(Operation.class);
        when(passwordService.setPasswordWithCustomPermissionCheck(ACCOUNT_ID, USER_CRN, password, new HashSet<>(),
                AuthorizationResourceAction.DESCRIBE_ENVIRONMENT)).thenReturn(operation);
        SyncOperationStatus status = mock(SyncOperationStatus.class);
        when(status.getStatus()).thenReturn(SynchronizationStatus.REJECTED);
        when(operationToSyncOperationStatus.convert(operation)).thenReturn(status);

        assertThrows(SyncOperationAlreadyRunningException.class, () -> ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.setPassword(request)));
    }
}
