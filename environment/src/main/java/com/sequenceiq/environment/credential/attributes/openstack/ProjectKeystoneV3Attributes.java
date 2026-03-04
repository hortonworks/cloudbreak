package com.sequenceiq.environment.credential.attributes.openstack;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class ProjectKeystoneV3Attributes extends KeystoneV3Base {

    private String projectDomainName;

    private String projectName;

    public String getProjectDomainName() {
        return projectDomainName;
    }

    public void setProjectDomainName(String projectDomainName) {
        this.projectDomainName = projectDomainName;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }
}
