package com.sequenceiq.cloudbreak.view;

import static com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup.IDENTITY_TYPE_ATTRIBUTE;

import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;

import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.domain.SecurityGroup;
import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.domain.stack.instance.network.InstanceGroupNetwork;
import com.sequenceiq.common.api.type.InstanceGroupType;
import com.sequenceiq.common.api.type.ScalabilityOption;
import com.sequenceiq.common.model.CloudIdentityType;

public interface InstanceGroupView {

    String getGroupName();

    InstanceGroupType getInstanceGroupType();

    Long getId();

    Template getTemplate();

    SecurityGroup getSecurityGroup();

    Json getAttributes();

    int getMinimumNodeCount();

    InstanceGroupNetwork getInstanceGroupNetwork();

    ScalabilityOption getScalabilityOption();

    default Optional<CloudIdentityType> getCloudIdentityType() {
        Json attributes = getAttributes();
        if (attributes != null && StringUtils.isNotEmpty(attributes.getValue())) {
            Map<String, Object> attributeMap = attributes.getMap();
            if (attributeMap.containsKey(IDENTITY_TYPE_ATTRIBUTE)) {
                return Optional.of(CloudIdentityType.valueOf(attributeMap.get(IDENTITY_TYPE_ATTRIBUTE).toString()));
            }
        }
        return Optional.empty();
    }
}
