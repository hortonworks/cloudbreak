package com.sequenceiq.freeipa.repository;

import java.util.Optional;

import javax.transaction.Transactional;

import org.springframework.data.repository.CrudRepository;

import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.entity.UserSyncStatus;

@Transactional(Transactional.TxType.REQUIRED)
public interface UserSyncStatusRepository extends CrudRepository<UserSyncStatus, Long> {

    Optional<UserSyncStatus> getByStack(Stack stack);

}
