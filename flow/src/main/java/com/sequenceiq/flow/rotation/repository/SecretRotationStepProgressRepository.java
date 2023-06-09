package com.sequenceiq.flow.rotation.repository;

import java.util.Set;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.data.repository.CrudRepository;

import com.sequenceiq.cloudbreak.rotation.secret.RotationFlowExecutionType;
import com.sequenceiq.cloudbreak.rotation.secret.SecretType;
import com.sequenceiq.flow.rotation.entity.SecretRotationStepProgress;

@Transactional(TxType.REQUIRED)
public interface SecretRotationStepProgressRepository extends CrudRepository<SecretRotationStepProgress, Long> {

    Set<SecretRotationStepProgress> findAllByResourceCrnAndExecutionType(String resourceCrn, RotationFlowExecutionType executionType);

    void deleteByResourceCrnAndSecretType(String resourceCrn, SecretType secretType);
}
