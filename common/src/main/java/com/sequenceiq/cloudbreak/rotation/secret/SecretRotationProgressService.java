package com.sequenceiq.cloudbreak.rotation.secret;

import java.util.Optional;

import com.sequenceiq.cloudbreak.rotation.secret.step.SecretRotationStep;

public interface SecretRotationProgressService<E> {

    boolean isFinished(E entity);

    void finished(E entity);

    Optional<E> latestStep(String resourceCrn, SecretType secretType, SecretRotationStep step, RotationFlowExecutionType executionType);

    void deleteAll(String resourceCrn, SecretType secretType);

}
