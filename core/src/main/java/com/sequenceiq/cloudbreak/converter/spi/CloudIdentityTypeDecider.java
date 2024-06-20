package com.sequenceiq.cloudbreak.converter.spi;

import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cmtemplate.configproviders.knox.KnoxRoles;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.common.model.CloudIdentityType;

@Component
public class CloudIdentityTypeDecider {

    public CloudIdentityType getIdentityType(Set<String> components) {
        if (components != null && components.contains(KnoxRoles.IDBROKER)) {
            return CloudIdentityType.ID_BROKER;
        }
        return CloudIdentityType.LOG;
    }

    public CloudIdentityType getIdentityTypeForInstanceGroup(String instanceGroupName, Map<String, Set<String>> componentsByHostGroup) {
        Set<String> components = componentsByHostGroup.get(instanceGroupName);
        if (components == null) {
            throw new CloudbreakServiceException(String.format("Could not determine CloudIdentityType for instance group with name '%s'!", instanceGroupName));
        }
        return getIdentityType(components);
    }
}
