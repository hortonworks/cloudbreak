package com.sequenceiq.cloudbreak.rotation.repository;

import java.util.Optional;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.data.repository.CrudRepository;

import com.sequenceiq.cloudbreak.rotation.SecretType;
import com.sequenceiq.cloudbreak.rotation.entity.SecretRotationStepProgress;
import com.sequenceiq.cloudbreak.workspace.repository.EntityType;

@EntityType(entityClass = SecretRotationStepProgress.class)
@Transactional(TxType.REQUIRED)
public interface SecretRotationStepProgressRepository extends CrudRepository<SecretRotationStepProgress, Long> {

    Optional<SecretRotationStepProgress> findByResourceCrnAndSecretType(String resourceCrn, SecretType secretType);

    void deleteByResourceCrnAndSecretType(String resourceCrn, SecretType secretType);

    void deleteAllByResourceCrn(String resourceCrn);
}
