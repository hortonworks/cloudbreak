package com.sequenceiq.environment.credential.attributes.openstack;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class KeystoneV3Attributes implements Serializable {

    private ProjectKeystoneV3Attributes project;

    private DomainKeystoneV3Attributes domain;

    public ProjectKeystoneV3Attributes getProject() {
        return project;
    }

    public void setProject(ProjectKeystoneV3Attributes project) {
        this.project = project;
    }

    public DomainKeystoneV3Attributes getDomain() {
        return domain;
    }

    public void setDomain(DomainKeystoneV3Attributes domain) {
        this.domain = domain;
    }
}
