package com.sequenceiq.cloudbreak.api.model;

import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
public class InstanceGroupResponse extends InstanceGroupBase {

    @ApiModelProperty(ModelDescriptions.ID)
    private Long id;
    @ApiModelProperty(ModelDescriptions.InstanceGroupModelDescription.METADATA)
    private Set<InstanceMetaDataJson> metadata = new HashSet<>();

    @JsonProperty("id")
    public Long getId() {
        return id;
    }

    @JsonIgnore
    public void setId(Long id) {
        this.id = id;
    }

    @JsonProperty("metadata")
    public Set<InstanceMetaDataJson> getMetadata() {
        return metadata;
    }

    @JsonIgnore
    public void setMetadata(Set<InstanceMetaDataJson> metadata) {
        this.metadata = metadata;
    }
}
