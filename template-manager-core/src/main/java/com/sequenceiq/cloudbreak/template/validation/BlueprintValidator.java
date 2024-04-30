package com.sequenceiq.cloudbreak.template.validation;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.view.InstanceGroupView;

public interface BlueprintValidator {

    void validate(Blueprint blueprint, Set<HostGroup> hostGroups, Collection<InstanceGroupView> instanceGroups,
        boolean validateServiceCardinality)
            throws BlueprintValidationException;

    void validateHostGroupScalingRequest(String accountId, Blueprint blueprint, Optional<ClouderaManagerProduct> cdhProduct,
            String hostGroupName, Integer adjustment, Collection<InstanceGroup> instanceGroups, boolean forced)
            throws BlueprintValidationException;

}
