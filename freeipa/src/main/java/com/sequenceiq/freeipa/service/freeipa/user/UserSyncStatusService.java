package com.sequenceiq.freeipa.service.freeipa.user;

import javax.inject.Inject;

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

    public UserSyncStatus getOrCreateForStack(Stack stack) {
        return userSyncStatusRepository.getByStack(stack).orElseGet(() -> {
            UserSyncStatus userSyncStatus = new UserSyncStatus();
            userSyncStatus.setStack(stack);
            userSyncStatus.setUmsEventGenerationIds(new Json(new UmsEventGenerationIds()));
            return userSyncStatusRepository.save(userSyncStatus);
        });
    }
}
