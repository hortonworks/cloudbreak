package com.sequenceiq.cloudbreak.api.endpoint.v4.blueprint.responses;

import java.util.HashMap;
import java.util.Map;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.BlueprintBasedUpgradeOption;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.CompactViewV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.ResourceStatus;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.BlueprintModelDescription;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
public class BlueprintV4ViewResponse extends CompactViewV4Response {
    @Schema(description = BlueprintModelDescription.STACK_TYPE)
    private String stackType;

    @Schema(description = BlueprintModelDescription.STACK_VERSION)
    private String stackVersion;

    @Schema(description = BlueprintModelDescription.HOST_GROUP_COUNT)
    private Integer hostGroupCount;

    @Schema(description = BlueprintModelDescription.STATUS)
    private ResourceStatus status;

    @Schema(description = BlueprintModelDescription.TAGS)
    private Map<String, Object> tags = new HashMap<>();

    @Schema(description = BlueprintModelDescription.UPGRADEABLE)
    private BlueprintBasedUpgradeOption upgradeable;

    private Long created;

    private Long lastUpdated;

    @NotNull
    @Size(max = 100, min = 1, message = "The length of the blueprint's name has to be in range of 1 to 100 and should not contain semicolon "
            + "and percentage character.")
    @Pattern(regexp = "^[^;\\/%]*$")
    public String getName() {
        return super.getName();
    }

    public String getStackType() {
        return stackType;
    }

    public void setStackType(String stackType) {
        this.stackType = stackType;
    }

    public String getStackVersion() {
        return stackVersion;
    }

    public void setStackVersion(String stackVersion) {
        this.stackVersion = stackVersion;
    }

    public Integer getHostGroupCount() {
        return hostGroupCount;
    }

    public void setHostGroupCount(Integer hostGroupCount) {
        this.hostGroupCount = hostGroupCount;
    }

    public ResourceStatus getStatus() {
        return status;
    }

    public void setStatus(ResourceStatus status) {
        this.status = status;
    }

    public Map<String, Object> getTags() {
        return tags;
    }

    public void setTags(Map<String, Object> tags) {
        this.tags = tags;
    }

    public Long getCreated() {
        return created;
    }

    public void setCreated(Long created) {
        this.created = created;
    }

    public BlueprintBasedUpgradeOption isUpgradeable() {
        return upgradeable;
    }

    public void setUpgradeable(BlueprintBasedUpgradeOption upgradeable) {
        this.upgradeable = upgradeable;
    }

    public Long getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(Long lastUpdated) {
        this.lastUpdated = lastUpdated;
    }
}
