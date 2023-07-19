package com.sequenceiq.cloudbreak.rotation;

import java.util.Map;

import com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;

public interface MultiSecretType extends SerializableRotationEnum {

    default SecretType getSecretTypeByResourceCrn(String resourceCrn) {
        CrnResourceDescriptor crnResourceDescriptorByCrn = CrnResourceDescriptor.getByCrnString(resourceCrn);
        if (crnResourceDescriptorByCrn.equals(parentCrnDescriptor())) {
            return parentSecretType();
        } else if (childSecretTypesByDescriptor().containsKey(crnResourceDescriptorByCrn)) {
            return childSecretTypesByDescriptor().get(crnResourceDescriptorByCrn);
        } else {
            throw new BadRequestException(String.format("Multi secret rotation for type %s is not possible by crn %s", value(), resourceCrn));
        }
    }

    SecretType parentSecretType();

    CrnResourceDescriptor parentCrnDescriptor();

    Map<CrnResourceDescriptor, SecretType> childSecretTypesByDescriptor();
}
