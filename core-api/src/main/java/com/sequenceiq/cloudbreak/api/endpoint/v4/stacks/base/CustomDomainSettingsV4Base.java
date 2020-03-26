package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.common.model.JsonEntity;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.StackModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class CustomDomainSettingsV4Base implements JsonEntity {

    @ApiModelProperty(StackModelDescription.CUSTOM_DOMAIN)
    private String domainName;

    @ApiModelProperty(StackModelDescription.CUSTOM_HOSTNAME)
    private String hostname;

    @ApiModelProperty(StackModelDescription.CLUSTER_NAME_AS_SUBDOMAIN)
    private boolean clusterNameAsSubdomain;

    @ApiModelProperty(StackModelDescription.HOSTGROUP_NAME_AS_HOSTNAME)
    private boolean hostgroupNameAsHostname;

    public String getDomainName() {
        return domainName;
    }

    public void setDomainName(String domainName) {
        this.domainName = domainName;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public boolean isClusterNameAsSubdomain() {
        return clusterNameAsSubdomain;
    }

    public void setClusterNameAsSubdomain(boolean clusterNameAsSubdomain) {
        this.clusterNameAsSubdomain = clusterNameAsSubdomain;
    }

    public boolean isHostgroupNameAsHostname() {
        return hostgroupNameAsHostname;
    }

    public void setHostgroupNameAsHostname(boolean hostgroupNameAsHostname) {
        this.hostgroupNameAsHostname = hostgroupNameAsHostname;
    }
}
