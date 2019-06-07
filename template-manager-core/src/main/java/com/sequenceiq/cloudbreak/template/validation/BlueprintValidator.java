package com.sequenceiq.cloudbreak.template.validation;

import java.util.Collection;
import java.util.Set;

import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;

public interface BlueprintValidator {

    void validateBlueprintForStack(Blueprint blueprint, Set<HostGroup> hostGroups, Collection<InstanceGroup> instanceGroups)
            throws BlueprintValidationException;

    void validateHostGroupScalingRequest(Blueprint blueprint, HostGroup hostGroup, Integer adjustment)
            throws BlueprintValidationException;

}
