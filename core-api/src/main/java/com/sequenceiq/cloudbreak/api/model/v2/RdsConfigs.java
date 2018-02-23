package com.sequenceiq.cloudbreak.api.model.v2;

import java.util.HashSet;
import java.util.Set;

import javax.validation.Valid;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.cloudbreak.api.model.JsonEntity;
import com.sequenceiq.cloudbreak.api.model.RDSConfigRequest;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.ClusterModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel("RdsConfigs")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class RdsConfigs implements JsonEntity {
    @ApiModelProperty(ClusterModelDescription.RDSCONFIG_IDS)
    private Set<Long> ids = new HashSet<>();

    @Valid
    @ApiModelProperty(ClusterModelDescription.RDS_CONFIGS)
    private Set<RDSConfigRequest> configs = new HashSet<>();

    public Set<Long> getIds() {
        return ids;
    }

    public void setIds(Set<Long> ids) {
        this.ids = ids;
    }

    public Set<RDSConfigRequest> getConfigs() {
        return configs;
    }

    public void setConfigs(Set<RDSConfigRequest> configs) {
        this.configs = configs;
    }
}
