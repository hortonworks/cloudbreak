package com.sequenceiq.cloudbreak.template.validation;

import java.util.Collection;
import java.util.Set;

import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;

public interface BlueprintValidator {

    void validate(Blueprint blueprint, Set<HostGroup> hostGroups, Collection<InstanceGroup> instanceGroups,
        boolean validateServiceCardinality)
            throws BlueprintValidationException;

    void validateHostGroupScalingRequest(String accountId, Blueprint blueprint, HostGroup hostGroup, Integer adjustment,
            Collection<InstanceGroup> instanceGroups)
            throws BlueprintValidationException;

}
