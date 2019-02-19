package com.sequenceiq.cloudbreak.api.endpoint.v4.clusterdefinition.responses;

import java.util.HashMap;
import java.util.Map;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.CompactViewV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.ResourceStatus;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.ClusterDefinitionModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
public class ClusterDefinitionV4ViewResponse extends CompactViewV4Response {
    @ApiModelProperty(ClusterDefinitionModelDescription.STACK_TYPE)
    private String stackType;

    @ApiModelProperty(ClusterDefinitionModelDescription.STACK_VERSION)
    private String stackVersion;

    @ApiModelProperty(ClusterDefinitionModelDescription.HOST_GROUP_COUNT)
    private Integer hostGroupCount;

    @ApiModelProperty(ClusterDefinitionModelDescription.STATUS)
    private ResourceStatus status;

    @ApiModelProperty(ClusterDefinitionModelDescription.TAGS)
    private Map<String, Object> tags = new HashMap<>();

    @NotNull
    @Size(max = 100, min = 1, message = "The length of the cluster definition's name has to be in range of 1 to 100 and should not contain semicolon "
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
}
