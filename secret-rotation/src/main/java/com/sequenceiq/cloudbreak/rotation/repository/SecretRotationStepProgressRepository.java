package com.sequenceiq.cloudbreak.rotation.repository;

import java.util.List;
import java.util.Optional;

import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;

import org.springframework.data.repository.CrudRepository;

import com.sequenceiq.cloudbreak.rotation.SecretType;
import com.sequenceiq.cloudbreak.rotation.entity.SecretRotationStepProgress;
import com.sequenceiq.cloudbreak.workspace.repository.EntityType;

@EntityType(entityClass = SecretRotationStepProgress.class)
@Transactional(TxType.REQUIRED)
public interface SecretRotationStepProgressRepository extends CrudRepository<SecretRotationStepProgress, Long> {

    Optional<SecretRotationStepProgress> findByResourceCrnAndSecretType(String resourceCrn, SecretType secretType);

    List<SecretRotationStepProgress> findByResourceCrn(String resourceCrn);

    void deleteByResourceCrnAndSecretType(String resourceCrn, SecretType secretType);

    void deleteAllByResourceCrn(String resourceCrn);
}
