package com.sequenceiq.cloudbreak.rotation;

import static com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor.DATAHUB;
import static com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor.ENVIRONMENT;
import static com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor.VM_DATALAKE;

import java.util.Set;

import com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor;

public enum MultiSecretType implements SerializableRotationEnum {

    CM_SERVICE_SHARED_DB(VM_DATALAKE, Set.of(DATAHUB)),
    DEMO_MULTI_SECRET(ENVIRONMENT, Set.of(VM_DATALAKE, DATAHUB));

    private final CrnResourceDescriptor parentCrnDescriptor;

    private final Set<CrnResourceDescriptor> childrenCrnDescriptors;

    MultiSecretType(CrnResourceDescriptor parentCrnDescriptor, Set<CrnResourceDescriptor> childrenCrnDescriptors) {
        this.parentCrnDescriptor = parentCrnDescriptor;
        this.childrenCrnDescriptors = childrenCrnDescriptors;
    }

    public CrnResourceDescriptor getParentCrnDescriptor() {
        return parentCrnDescriptor;
    }

    public Set<CrnResourceDescriptor> getChildrenCrnDescriptors() {
        return childrenCrnDescriptors;
    }

    @Override
    public Class<? extends Enum<?>> getClazz() {
        return MultiSecretType.class;
    }

    @Override
    public String value() {
        return name();
    }
}
