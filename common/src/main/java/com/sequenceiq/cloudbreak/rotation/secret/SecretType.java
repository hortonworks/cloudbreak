package com.sequenceiq.cloudbreak.rotation.secret;

import java.util.List;

public interface SecretType {
    List<SecretRotationStep> getSteps();
}
