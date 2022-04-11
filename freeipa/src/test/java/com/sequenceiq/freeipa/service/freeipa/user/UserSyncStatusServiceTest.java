package com.sequenceiq.freeipa.service.freeipa.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.entity.UserSyncStatus;
import com.sequenceiq.freeipa.repository.UserSyncStatusRepository;

@ExtendWith(MockitoExtension.class)
class UserSyncStatusServiceTest {

    @Mock
    private UserSyncStatusRepository userSyncStatusRepository;

    @InjectMocks
    private UserSyncStatusService underTest;

    @Test
    public void testGetOrCreateForStackExists() {
        Stack stack = new Stack();
        UserSyncStatus userSyncStatus = new UserSyncStatus();
        when(userSyncStatusRepository.getByStack(stack)).thenReturn(Optional.of(userSyncStatus));

        UserSyncStatus result = underTest.getOrCreateForStack(stack);

        assertEquals(userSyncStatus, result);
    }

    @Test
    public void testGetOrCreateForStackDontExists() {
        Stack stack = new Stack();
        when(userSyncStatusRepository.getByStack(stack)).thenReturn(Optional.empty());
        when(userSyncStatusRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0, UserSyncStatus.class));

        UserSyncStatus result = underTest.getOrCreateForStack(stack);

        ArgumentCaptor<UserSyncStatus> captor = ArgumentCaptor.forClass(UserSyncStatus.class);
        verify(userSyncStatusRepository).save(captor.capture());
        UserSyncStatus saved = captor.getValue();
        assertEquals(saved, result);
        assertEquals(stack, saved.getStack());
        assertNotNull(saved.getUmsEventGenerationIds());
    }

}