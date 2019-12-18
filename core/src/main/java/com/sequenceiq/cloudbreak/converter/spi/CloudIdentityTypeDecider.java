package com.sequenceiq.cloudbreak.converter.spi;

import java.util.Set;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cmtemplate.configproviders.knox.KnoxRoles;
import com.sequenceiq.common.model.CloudIdentityType;

@Component
class CloudIdentityTypeDecider {

    CloudIdentityType getIdentityType(Set<String> components) {
        if (components != null && components.contains(KnoxRoles.IDBROKER)) {
            return CloudIdentityType.ID_BROKER;
        }
        return CloudIdentityType.LOG;
    }
}
