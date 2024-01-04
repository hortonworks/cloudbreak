package com.sequenceiq.freeipa.repository;

import java.util.Optional;

import jakarta.transaction.Transactional;

import org.springframework.data.repository.CrudRepository;

import com.sequenceiq.freeipa.entity.UserSyncStatus;

@Transactional(Transactional.TxType.REQUIRED)
public interface UserSyncStatusRepository extends CrudRepository<UserSyncStatus, Long> {

    Optional<UserSyncStatus> findByStackId(Long stackId);

}
