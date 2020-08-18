package com.sequenceiq.environment.credential.v1.converter.openstack;

import static com.sequenceiq.cloudbreak.util.NullUtil.doIfNotNull;

import org.springframework.stereotype.Component;

import com.sequenceiq.environment.api.v1.credential.model.parameters.openstack.DomainKeystoneV3Parameters;
import com.sequenceiq.environment.api.v1.credential.model.parameters.openstack.KeystoneV2Parameters;
import com.sequenceiq.environment.api.v1.credential.model.parameters.openstack.KeystoneV3Parameters;
import com.sequenceiq.environment.api.v1.credential.model.parameters.openstack.OpenstackParameters;
import com.sequenceiq.environment.api.v1.credential.model.parameters.openstack.ProjectKeystoneV3Parameters;
import com.sequenceiq.environment.credential.attributes.openstack.DomainKeystoneV3Attributes;
import com.sequenceiq.environment.credential.attributes.openstack.KeystoneV2Attributes;
import com.sequenceiq.environment.credential.attributes.openstack.KeystoneV3Attributes;
import com.sequenceiq.environment.credential.attributes.openstack.OpenStackCredentialAttributes;
import com.sequenceiq.environment.credential.attributes.openstack.ProjectKeystoneV3Attributes;

@Component
public class OpenStackCredentialV1ParametersToOpenStackCredentialAttributesConverter {

    public OpenStackCredentialAttributes convert(OpenstackParameters source) {
        OpenStackCredentialAttributes response = new OpenStackCredentialAttributes();
        doIfNotNull(source.getKeystoneV2(), param -> response.setKeystoneV2(getKeystoneV2(param)));

        doIfNotNull(source.getKeystoneV3(), param -> response.setKeystoneV3(getKeystoneV3(param)));
        response.setEndpoint(source.getEndpoint());
        response.setFacing(source.getFacing());
        response.setPassword(source.getPassword());
        response.setUserName(source.getUserName());
        return response;
    }

    public OpenstackParameters convert(OpenStackCredentialAttributes source) {
        OpenstackParameters response = new OpenstackParameters();
        doIfNotNull(source.getKeystoneV2(), param -> response.setKeystoneV2(getKeystoneV2(param)));
        doIfNotNull(source.getKeystoneV3(), param -> response.setKeystoneV3(getKeystoneV3(param)));
        response.setEndpoint(source.getEndpoint());
        response.setFacing(source.getFacing());
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
