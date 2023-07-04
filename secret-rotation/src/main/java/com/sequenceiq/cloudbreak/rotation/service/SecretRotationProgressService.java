package com.sequenceiq.cloudbreak.rotation.service;

import java.util.Optional;

import com.sequenceiq.cloudbreak.rotation.RotationFlowExecutionType;
import com.sequenceiq.cloudbreak.rotation.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.SecretType;

public interface SecretRotationProgressService<E> {

    boolean isFinished(E entity);

    void finished(E entity);

    Optional<E> latestStep(String resourceCrn, SecretType secretType, SecretRotationStep step, RotationFlowExecutionType executionType);

    void deleteAll(String resourceCrn, SecretType secretType);

}
