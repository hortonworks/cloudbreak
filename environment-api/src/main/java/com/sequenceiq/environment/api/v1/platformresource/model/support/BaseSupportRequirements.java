package com.sequenceiq.environment.api.v1.platformresource.model.support;

import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class BaseSupportRequirements {

    private Set<String> missingDefaultX86InstancesTypes = new HashSet<>();

    private Set<String> missingDefaultArmInstanceTypes = new HashSet<>();

    public Set<String> getMissingDefaultX86Instances() {
        return missingDefaultX86InstancesTypes;
    }

    public void setMissingDefaultX86InstancesTypes(Set<String> missingDefaultX86InstancesTypes) {
        this.missingDefaultX86InstancesTypes = missingDefaultX86InstancesTypes;
    }

    public Set<String> getMissingDefaultArmInstanceTypes() {
        return missingDefaultArmInstanceTypes;
    }

    public void setMissingDefaultArmInstanceTypes(Set<String> missingDefaultArmInstanceTypes) {
        this.missingDefaultArmInstanceTypes = missingDefaultArmInstanceTypes;
    }

    @Override
    public String toString() {
        return "BaseSupportRequirements{" +
                "missingDefaultX86InstancesTypes=" + missingDefaultX86InstancesTypes +
                ", missingDefaultArmInstanceTypes=" + missingDefaultArmInstanceTypes +
                '}';
    }
}
