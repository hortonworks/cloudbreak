package com.sequenceiq.freeipa.service.freeipa.user;

import java.util.Optional;

import jakarta.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.entity.UserSyncStatus;
import com.sequenceiq.freeipa.repository.UserSyncStatusRepository;
import com.sequenceiq.freeipa.service.freeipa.user.model.UmsEventGenerationIds;

@Service
public class UserSyncStatusService {

    @Inject
    private UserSyncStatusRepository userSyncStatusRepository;

    public UserSyncStatus save(UserSyncStatus userSyncStatus) {
        return userSyncStatusRepository.save(userSyncStatus);
    }

    public UserSyncStatus getOrCreateForStack(Long stackId) {
        return userSyncStatusRepository.findByStackId(stackId).orElseGet(() -> createNewUserSyncStatusForStack(stackId));
    }

    private UserSyncStatus createNewUserSyncStatusForStack(Long stackId) {
        UserSyncStatus userSyncStatus = new UserSyncStatus();
        userSyncStatus.setStackId(stackId);
        userSyncStatus.setUmsEventGenerationIds(new Json(new UmsEventGenerationIds()));
        return userSyncStatusRepository.save(userSyncStatus);
    }

    public Optional<UserSyncStatus> findByStack(Stack stack) {
        return userSyncStatusRepository.findByStackId(stack.getId());
    }

}
