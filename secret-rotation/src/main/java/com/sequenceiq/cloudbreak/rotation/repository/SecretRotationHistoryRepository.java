package com.sequenceiq.cloudbreak.rotation.repository;

import java.util.List;
import java.util.Optional;

import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;

import org.springframework.data.repository.CrudRepository;

import com.sequenceiq.cloudbreak.rotation.SecretType;
import com.sequenceiq.cloudbreak.rotation.entity.SecretRotationHistory;
import com.sequenceiq.cloudbreak.workspace.repository.EntityType;

@EntityType(entityClass = SecretRotationHistory.class)
@Transactional(TxType.REQUIRED)
public interface SecretRotationHistoryRepository extends CrudRepository<SecretRotationHistory, Long> {

    Optional<SecretRotationHistory> findByResourceCrnAndSecretType(String resourceCrn, SecretType secretType);

    List<SecretRotationHistory> findByResourceCrn(String resourceCrn);
}
