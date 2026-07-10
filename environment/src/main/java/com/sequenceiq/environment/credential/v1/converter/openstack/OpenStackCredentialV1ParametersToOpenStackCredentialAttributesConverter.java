package com.sequenceiq.environment.credential.v1.converter.openstack;

import static com.sequenceiq.cloudbreak.util.NullUtil.doIfNotNull;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.environment.api.v1.credential.model.parameters.openstack.DomainKeystoneV3Parameters;
import com.sequenceiq.environment.api.v1.credential.model.parameters.openstack.KeystoneV3Parameters;
import com.sequenceiq.environment.api.v1.credential.model.parameters.openstack.OpenstackParameters;
import com.sequenceiq.environment.api.v1.credential.model.parameters.openstack.ProjectKeystoneV3Parameters;
import com.sequenceiq.environment.credential.attributes.openstack.DomainKeystoneV3Attributes;
import com.sequenceiq.environment.credential.attributes.openstack.KeystoneV3Attributes;
import com.sequenceiq.environment.credential.attributes.openstack.OpenStackCredentialAttributes;
import com.sequenceiq.environment.credential.attributes.openstack.ProjectKeystoneV3Attributes;

@Component
public class OpenStackCredentialV1ParametersToOpenStackCredentialAttributesConverter {

    public OpenStackCredentialAttributes convert(OpenstackParameters source) {
        if (source.getKeystoneV3() == null && source.getKeystoneV2() != null) {
            throw new BadRequestException("Keystone V2 is not supported, please use Keystone V3.");
        }
        OpenStackCredentialAttributes response = new OpenStackCredentialAttributes();
        doIfNotNull(source.getKeystoneV3(), param -> response.setKeystoneV3(toKeystoneV3Attributes(param)));
        response.setEndpoint(source.getEndpoint());
        response.setFacing(source.getFacing());
        response.setPassword(source.getPassword());
        response.setUserName(source.getUserName());
        return response;
    }

    public OpenstackParameters convert(OpenStackCredentialAttributes source) {
        OpenstackParameters response = new OpenstackParameters();
        doIfNotNull(source.getKeystoneV3(), param -> response.setKeystoneV3(toKeystoneV3Attributes(param)));
        response.setEndpoint(source.getEndpoint());
        response.setFacing(source.getFacing());
        response.setPassword(source.getPassword());
        response.setUserName(source.getUserName());
        return response;
    }

    private KeystoneV3Attributes toKeystoneV3Attributes(KeystoneV3Parameters keystoneV3) {
        KeystoneV3Attributes response = new KeystoneV3Attributes();
        if (keystoneV3.getDomain() == null && keystoneV3.getProject() == null) {
            throw new BadRequestException("Either domain or project must be set for Keystone V3");
        }
        if (keystoneV3.getDomain() != null) {
            response.setDomain(toDomainKeystoneV3Attributes(keystoneV3.getDomain()));
        }
        if (keystoneV3.getProject() != null) {
            response.setProject(toProjectKeystoneV3Attributes(keystoneV3.getProject()));
        }
        return response;
    }

    private ProjectKeystoneV3Attributes toProjectKeystoneV3Attributes(ProjectKeystoneV3Parameters project) {
        ProjectKeystoneV3Attributes response = new ProjectKeystoneV3Attributes();
        response.setProjectDomainName(project.getProjectDomainName());
        response.setProjectName(project.getProjectName());
        response.setUserDomain(project.getUserDomain());
        return response;
    }

    private DomainKeystoneV3Attributes toDomainKeystoneV3Attributes(DomainKeystoneV3Parameters domain) {
        DomainKeystoneV3Attributes response = new DomainKeystoneV3Attributes();
        response.setDomainName(domain.getDomainName());
        response.setUserDomain(domain.getUserDomain());
        return response;
    }

    private KeystoneV3Parameters toKeystoneV3Attributes(KeystoneV3Attributes keystoneV3) {
        KeystoneV3Parameters response = new KeystoneV3Parameters();
        if (keystoneV3.getDomain() != null) {
            response.setDomain(toDomainKeystoneV3Attributes(keystoneV3.getDomain()));
        }
        if (keystoneV3.getProject() != null) {
            response.setProject(toProjectKeystoneV3Attributes(keystoneV3.getProject()));
        }
        return response;
    }

    private ProjectKeystoneV3Parameters toProjectKeystoneV3Attributes(ProjectKeystoneV3Attributes project) {
        ProjectKeystoneV3Parameters response = new ProjectKeystoneV3Parameters();
        response.setProjectDomainName(project.getProjectDomainName());
        response.setProjectName(project.getProjectName());
        response.setUserDomain(project.getUserDomain());
        return response;
    }

    private DomainKeystoneV3Parameters toDomainKeystoneV3Attributes(DomainKeystoneV3Attributes domain) {
        DomainKeystoneV3Parameters response = new DomainKeystoneV3Parameters();
        response.setDomainName(domain.getDomainName());
        response.setUserDomain(domain.getUserDomain());
        return response;
    }
}
