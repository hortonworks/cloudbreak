package com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.parameters.openstack;

import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.MappableBase;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class KeystoneV3Parameters extends MappableBase {

    @ApiModelProperty
    private ProjectKeystoneV3Parameters project;

    @ApiModelProperty
    private DomainKeystoneV3Parameters domain;

    public ProjectKeystoneV3Parameters getProject() {
        return project;
    }

    public void setProject(ProjectKeystoneV3Parameters project) {
        this.project = project;
    }

    public DomainKeystoneV3Parameters getDomain() {
        return domain;
    }

    public void setDomain(DomainKeystoneV3Parameters domain) {
        this.domain = domain;
    }

    @Override
    public Map<String, Object> asMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("keystoneVersion", KeystoneVersionTypes.V3.getType());
        if (project != null) {
            map.putAll(project.asMap());
        } else if (domain != null) {
            map.putAll(domain.asMap());
        }
        return map;
    }

    @Override
    @JsonIgnore
    @ApiModelProperty(hidden = true)
    public CloudPlatform getCloudPlatform() {
        return CloudPlatform.OPENSTACK;
    }

    @Override
    public void parse(Map<String, Object> parameters) {
        super.parse(parameters);
        project = new ProjectKeystoneV3Parameters();
        project.parse(parameters);
        domain = new DomainKeystoneV3Parameters();
        domain.parse(parameters);
    }
}
