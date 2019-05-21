package com.sequenceiq.environment.credential.converter;

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
        if (source == null) {
            return null;
        }
        OpenStackCredentialAttributes response = new OpenStackCredentialAttributes();
        response.setEndpoint(source.getEndpoint());
        response.setFacing(source.getFacing());
        response.setKeystoneV2(getKeystoneV2(source.getKeystoneV2()));
        response.setKeystoneV3(getKeystoneV3(source.getKeystoneV3()));
        response.setPassword(source.getPassword());
        response.setUserName(source.getUserName());
        return response;
    }

    public OpenstackParameters convert(OpenStackCredentialAttributes source) {
        if (source == null) {
            return null;
        }
        OpenstackParameters response = new OpenstackParameters();
        response.setEndpoint(source.getEndpoint());
        response.setFacing(source.getFacing());
        response.setKeystoneV2(getKeystoneV2(source.getKeystoneV2()));
        response.setKeystoneV3(getKeystoneV3(source.getKeystoneV3()));
        response.setPassword(source.getPassword());
        response.setUserName(source.getUserName());
        return response;
    }

    private KeystoneV2Attributes getKeystoneV2(KeystoneV2Parameters source) {
        if (source == null) {
            return null;
        }
        KeystoneV2Attributes response = new KeystoneV2Attributes();
        response.setTenantName(source.getTenantName());
        return response;
    }

    private KeystoneV3Attributes getKeystoneV3(KeystoneV3Parameters source) {
        if (source == null) {
            return null;
        }
        KeystoneV3Attributes response = new KeystoneV3Attributes();
        response.setDomain(getDomain(source.getDomain()));
        response.setProject(getProject(source.getProject()));
        return response;
    }

    private ProjectKeystoneV3Attributes getProject(ProjectKeystoneV3Parameters source) {
        if (source == null) {
            return null;
        }
        ProjectKeystoneV3Attributes response = new ProjectKeystoneV3Attributes();
        response.setProjectDomainName(source.getProjectDomainName());
        response.setProjectName(source.getProjectName());
        response.setUserDomain(source.getUserDomain());
        return response;
    }

    private DomainKeystoneV3Attributes getDomain(DomainKeystoneV3Parameters source) {
        if (source == null) {
            return null;
        }
        DomainKeystoneV3Attributes response = new DomainKeystoneV3Attributes();
        response.setDomainName(source.getDomainName());
        response.setUserDomain(source.getUserDomain());
        return response;
    }

    private KeystoneV2Parameters getKeystoneV2(KeystoneV2Attributes source) {
        if (source == null) {
            return null;
        }
        KeystoneV2Parameters response = new KeystoneV2Parameters();
        response.setTenantName(source.getTenantName());
        return response;
    }

    private KeystoneV3Parameters getKeystoneV3(KeystoneV3Attributes source) {
        if (source == null) {
            return null;
        }
        KeystoneV3Parameters response = new KeystoneV3Parameters();
        response.setDomain(getDomain(source.getDomain()));
        response.setProject(getProject(source.getProject()));
        return response;
    }

    private ProjectKeystoneV3Parameters getProject(ProjectKeystoneV3Attributes source) {
        if (source == null) {
            return null;
        }
        ProjectKeystoneV3Parameters response = new ProjectKeystoneV3Parameters();
        response.setProjectDomainName(source.getProjectDomainName());
        response.setProjectName(source.getProjectName());
        response.setUserDomain(source.getUserDomain());
        return response;
    }

    private DomainKeystoneV3Parameters getDomain(DomainKeystoneV3Attributes source) {
        if (source == null) {
            return null;
        }
        DomainKeystoneV3Parameters response = new DomainKeystoneV3Parameters();
        response.setDomainName(source.getDomainName());
        response.setUserDomain(source.getUserDomain());
        return response;
    }
}
