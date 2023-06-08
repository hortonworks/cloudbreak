package com.sequenceiq.cloudbreak.rotation.secret;

import java.util.List;

import com.sequenceiq.cloudbreak.rotation.secret.step.SecretRotationStep;

public interface SecretType {
    List<SecretRotationStep> getSteps();
}
