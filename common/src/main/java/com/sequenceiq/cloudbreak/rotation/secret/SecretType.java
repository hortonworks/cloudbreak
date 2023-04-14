package com.sequenceiq.cloudbreak.rotation.secret;

import static com.sequenceiq.cloudbreak.rotation.secret.SecretLocationType.CM_USER;
import static com.sequenceiq.cloudbreak.rotation.secret.SecretLocationType.VAULT;

import java.util.List;

public enum SecretType {
    CLOUDBREAK_CM_ADMIN_PASSWORD(List.of(VAULT, CM_USER), PostRotationAction.REPAIR),
    MGMT_CM_ADMIN_PASSWORD(List.of(VAULT, CM_USER), PostRotationAction.REPAIR),
    SERVICES_DB_PASSWORDS(List.of(VAULT), PostRotationAction.REPAIR);

    private final List<SecretLocationType> rotations;

    private final PostRotationAction postRotationAction;

    SecretType(List<SecretLocationType> rotations, PostRotationAction postRotationAction) {
        this.rotations = rotations;
        this.postRotationAction = postRotationAction;
    }

    public List<SecretLocationType> getRotations() {
        return rotations;
    }

    public PostRotationAction getPostRotationAction() {
        return postRotationAction;
    }
}
