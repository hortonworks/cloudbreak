package com.sequenceiq.freeipa.service.freeipa.user;

import com.sequenceiq.freeipa.entity.Operation;
import com.sequenceiq.freeipa.repository.OperationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserSyncAcceptorTest {

    @Mock
    private OperationRepository operationRepository;

    @InjectMocks
    private UserSyncAcceptor userSyncAcceptor;

    @Test
    void testSingleUserAlwaysAcccepted() {
        Operation syncOperation = mock(Operation.class);
        when(syncOperation.getUserList()).thenReturn(List.of("userA"));
        Operation runningSyncOperation = mock(Operation.class);
        assertFalse(userSyncAcceptor.doOperationsConflict(syncOperation, runningSyncOperation));
    }

    @Test
    void testUserSyncAcceptedWithDifferentEnvironment() {
        Operation syncOperation = mock(Operation.class);
        when(syncOperation.getUserList()).thenReturn(List.of("userA", "userB"));
        when(syncOperation.getEnvironmentList()).thenReturn(List.of("envA"));

        Operation runningSyncOperation = mock(Operation.class);
        when(runningSyncOperation.getUserList()).thenReturn(List.of("userA"));
        when(runningSyncOperation.getEnvironmentList()).thenReturn(List.of("envB"));

        assertFalse(userSyncAcceptor.doOperationsConflict(syncOperation, runningSyncOperation));
    }

    @Test
    void testSingleUserSyncRejectedOnUserAndEnvOverlap() {
        Operation syncOperation = mock(Operation.class);
        when(syncOperation.getUserList()).thenReturn(List.of("userA", "userB"));
        when(syncOperation.getEnvironmentList()).thenReturn(List.of("envA"));

        Operation runningSyncOperation = mock(Operation.class);
        when(runningSyncOperation.getUserList()).thenReturn(List.of("userA"));
        when(runningSyncOperation.getEnvironmentList()).thenReturn(List.of("envA"));

        assertTrue(userSyncAcceptor.doOperationsConflict(syncOperation, runningSyncOperation));
    }
}