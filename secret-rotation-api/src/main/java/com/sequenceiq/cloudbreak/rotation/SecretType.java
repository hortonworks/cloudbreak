package com.sequenceiq.cloudbreak.rotation;

import java.util.List;

public interface SecretType {
    List<SecretRotationStep> getSteps();
}
