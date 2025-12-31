package com.sequenceiq.common.model.support;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public abstract class AbstractPlatformSupportRequirements {

    private Set<String> defaultArmInstanceTypeRequirements = new HashSet<>();

    private Set<String> defaultX86InstanceTypeRequirements = new HashSet<>();

    public Set<String> getDefaultArmInstanceTypeRequirements() {
        return defaultArmInstanceTypeRequirements;
    }

    public void setDefaultArmInstanceTypeRequirements(Set<String> defaultArmInstanceTypeRequirements) {
        this.defaultArmInstanceTypeRequirements = defaultArmInstanceTypeRequirements;
    }

    public Set<String> getDefaultX86InstanceTypeRequirements() {
        return defaultX86InstanceTypeRequirements;
    }

    public void setDefaultX86InstanceTypeRequirements(Set<String> defaultX86InstanceTypeRequirements) {
        this.defaultX86InstanceTypeRequirements = defaultX86InstanceTypeRequirements;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        AbstractPlatformSupportRequirements that = (AbstractPlatformSupportRequirements) o;
        return Objects.equals(defaultX86InstanceTypeRequirements, that.defaultX86InstanceTypeRequirements)
                && Objects.equals(defaultArmInstanceTypeRequirements, that.defaultArmInstanceTypeRequirements);
    }

    @Override
    public int hashCode() {
        return Objects.hash(defaultX86InstanceTypeRequirements, defaultArmInstanceTypeRequirements);
    }

    @Override
    public String toString() {
        return "AbstractPlatformSupportRequirements{" +
                "defaultX86InstanceTypeRequirements=" + defaultX86InstanceTypeRequirements +
                ", defaultArmInstanceTypeRequirements=" + defaultArmInstanceTypeRequirements +
                '}';
    }
}
