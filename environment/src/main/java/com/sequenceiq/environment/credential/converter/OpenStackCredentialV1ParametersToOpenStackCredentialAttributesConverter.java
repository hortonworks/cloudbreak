package com.sequenceiq.environment.credential.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.environment.api.credential.model.parameters.openstack.DomainKeystoneV3Parameters;
import com.sequenceiq.environment.api.credential.model.parameters.openstack.KeystoneV2Parameters;
import com.sequenceiq.environment.api.credential.model.parameters.openstack.KeystoneV3Parameters;
import com.sequenceiq.environment.api.credential.model.parameters.openstack.OpenstackCredentialV1Parameters;
import com.sequenceiq.environment.api.credential.model.parameters.openstack.ProjectKeystoneV3Parameters;
import com.sequenceiq.environment.credential.attributes.openstack.DomainKeystoneV3Attributes;
import com.sequenceiq.environment.credential.attributes.openstack.KeystoneV2Attributes;
import com.sequenceiq.environment.credential.attributes.openstack.KeystoneV3Attributes;
import com.sequenceiq.environment.credential.attributes.openstack.OpenStackCredentialAttributes;
import com.sequenceiq.environment.credential.attributes.openstack.ProjectKeystoneV3Attributes;

@Component
public class OpenStackCredentialV1ParametersToOpenStackCredentialAttributesConverter {

    public OpenStackCredentialAttributes convert(OpenstackCredentialV1Parameters source) {
        OpenStackCredentialAttributes response = new OpenStackCredentialAttributes();
        response.setEndpoint(source.getEndpoint());
        response.setFacing(source.getFacing());
        response.setKeystoneV2(getKeystoneV2(source.getKeystoneV2()));
        response.setKeystoneV3(getKeystoneV3(source.getKeystoneV3()));
        response.setPassword(source.getPassword());
        response.setUserName(source.getUserName());
        return response;
    }

    public OpenstackCredentialV1Parameters convert(OpenStackCredentialAttributes source) {
        OpenstackCredentialV1Parameters response = new OpenstackCredentialV1Parameters();
        response.setEndpoint(source.getEndpoint());
        response.setFacing(source.getFacing());
        response.setKeystoneV2(getKeystoneV2(source.getKeystoneV2()));
        response.setKeystoneV3(getKeystoneV3(source.getKeystoneV3()));
        response.setPassword(source.getPassword());
        response.setUserName(source.getUserName());
        return response;
    }

    private KeystoneV2Attributes getKeystoneV2(KeystoneV2Parameters keystoneV2) {
        KeystoneV2Attributes response = new KeystoneV2Attributes();
        response.setTenantName(keystoneV2.getTenantName());
        return response;
    }

    private KeystoneV3Attributes getKeystoneV3(KeystoneV3Parameters keystoneV3) {
        KeystoneV3Attributes response = new KeystoneV3Attributes();
        response.setDomain(getDomain(keystoneV3.getDomain()));
        response.setProject(getProject(keystoneV3.getProject()));
        return response;
    }

    private ProjectKeystoneV3Attributes getProject(ProjectKeystoneV3Parameters project) {
        ProjectKeystoneV3Attributes response = new ProjectKeystoneV3Attributes();
        response.setProjectDomainName(project.getProjectDomainName());
        response.setProjectName(project.getProjectName());
        response.setUserDomain(project.getUserDomain());
        return response;
    }

    private DomainKeystoneV3Attributes getDomain(DomainKeystoneV3Parameters domain) {
        DomainKeystoneV3Attributes response = new DomainKeystoneV3Attributes();
        response.setDomainName(domain.getDomainName());
        response.setUserDomain(domain.getUserDomain());
        return response;
    }

    private KeystoneV2Parameters getKeystoneV2(KeystoneV2Attributes keystoneV2) {
        KeystoneV2Parameters response = new KeystoneV2Parameters();
        response.setTenantName(keystoneV2.getTenantName());
        return response;
    }

    private KeystoneV3Parameters getKeystoneV3(KeystoneV3Attributes keystoneV3) {
        KeystoneV3Parameters response = new KeystoneV3Parameters();
        response.setDomain(getDomain(keystoneV3.getDomain()));
        response.setProject(getProject(keystoneV3.getProject()));
        return response;
    }

    private ProjectKeystoneV3Parameters getProject(ProjectKeystoneV3Attributes project) {
        ProjectKeystoneV3Parameters response = new ProjectKeystoneV3Parameters();
        response.setProjectDomainName(project.getProjectDomainName());
        response.setProjectName(project.getProjectName());
        response.setUserDomain(project.getUserDomain());
        return response;
    }

    private DomainKeystoneV3Parameters getDomain(DomainKeystoneV3Attributes domain) {
        DomainKeystoneV3Parameters response = new DomainKeystoneV3Parameters();
        response.setDomainName(domain.getDomainName());
        response.setUserDomain(domain.getUserDomain());
        return response;
    }
}
