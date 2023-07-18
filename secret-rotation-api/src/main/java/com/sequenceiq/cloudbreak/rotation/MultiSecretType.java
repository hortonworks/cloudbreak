package com.sequenceiq.cloudbreak.rotation;

import java.util.List;

import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;

public interface MultiSecretType extends SerializableRotationEnum {

    default List<SecretType> getMultiSecretTypes() {
        return List.of(parentSecretType(), childSecretType());
    }

    default SecretType getSecretTypeByResourceCrn(String resourceCrn) {
        if (Crn.safeFromString(resourceCrn).getResourceType().equals(parentCrnDescriptor().getResourceType())) {
            return parentSecretType();
        } else if (Crn.safeFromString(resourceCrn).getResourceType().equals(childCrnDescriptor().getResourceType())) {
            return childSecretType();
        } else {
            throw new BadRequestException(String.format("Multi secret rotation for type %s is not possible by crn %s", value(), resourceCrn));
        }
    }

    SecretType parentSecretType();

    SecretType childSecretType();

    CrnResourceDescriptor parentCrnDescriptor();

    CrnResourceDescriptor childCrnDescriptor();
}
