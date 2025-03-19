package com.sequenceiq.freeipa.api.v1.freeipa.stack.model.scale;

import static com.sequenceiq.freeipa.api.v1.freeipa.stack.doc.FreeIpaModelDescriptions.FORCE;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.cloudbreak.validation.MutuallyExclusiveNotNull;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.AvailabilityType;
import com.sequenceiq.service.api.doc.ModelDescriptions;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "FreeIpaDownscaleV1Request")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@MutuallyExclusiveNotNull(fieldGroups = {"targetAvailabilityType", "instanceIds"}, message = "Either targetAvailabilityType or instanceIds should be " +
        "provided but not both.")
public class DownscaleRequest extends ScaleRequestBase {

    @Schema(description = ModelDescriptions.AVAILABILITY_TYPE)
    private AvailabilityType targetAvailabilityType;

    @Schema(description = ModelDescriptions.INSTANCE_ID)
    private Set<String> instanceIds;

    @Schema(description = FORCE)
    private boolean force;

    public AvailabilityType getTargetAvailabilityType() {
        return targetAvailabilityType;
    }

    public void setTargetAvailabilityType(AvailabilityType targetAvailabilityType) {
        this.targetAvailabilityType = targetAvailabilityType;
    }

    public Set<String> getInstanceIds() {
        return instanceIds;
    }

    public void setInstanceIds(Set<String> instanceIds) {
        this.instanceIds = instanceIds;
    }

    public boolean isForce() {
        return force;
    }

    public void setForce(boolean force) {
        this.force = force;
    }

    @Override
    public String toString() {
        return "DownscaleRequest{" +
                "targetAvailabilityType=" + targetAvailabilityType +
                ", instanceIds=" + instanceIds +
                ", force=" + force +
                "} " + super.toString();
    }
}
