package com.sequenceiq.cloudbreak.converter.v4.credentials;

import java.security.InvalidParameterException;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.parameters.openstack.DomainKeystoneV3Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.parameters.openstack.KeystoneV2Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.parameters.openstack.KeystoneV3Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.parameters.openstack.OpenstackCredentialV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.parameters.openstack.ProjectKeystoneV3Parameters;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;

@Component
public class MapToOpenstackCredentialV4ParametersConverter extends
        AbstractConversionServiceAwareConverter<Map<String, Object>, OpenstackCredentialV4Parameters> {

    @Override
    public OpenstackCredentialV4Parameters convert(Map<String, Object> source) {
        OpenstackCredentialV4Parameters parameters = new OpenstackCredentialV4Parameters();
        parameters.setPassword((String) source.get("password"));
        parameters.setEndpoint((String) source.get("endpoint"));
        parameters.setUserName((String) source.get("userName"));
        parameters.setFacing((String) source.get("facing"));
        if (source.get("userDomain") == null) {
            KeystoneV2Parameters v2Parameters = new KeystoneV2Parameters();
            v2Parameters.setTenantName((String) source.get("tenantName"));
            parameters.setKeystoneV2Parameters(v2Parameters);
        } else  {
            KeystoneV3Parameters v3Parameters = new KeystoneV3Parameters();
            if (source.get("projectDomainName") != null) {
                ProjectKeystoneV3Parameters projectKeystoneV3Parameters = new ProjectKeystoneV3Parameters();
                projectKeystoneV3Parameters.setProjectDomainName((String) source.get("projectDomainName"));
                projectKeystoneV3Parameters.setProjectName((String) source.get("projectName"));
                projectKeystoneV3Parameters.setUserDomain((String) source.get("userDomain"));
                v3Parameters.setProject(projectKeystoneV3Parameters);
            } else if (source.get("domainName") != null) {
                DomainKeystoneV3Parameters domainKeystoneV3Parameters = new DomainKeystoneV3Parameters();
                domainKeystoneV3Parameters.setUserDomain((String) source.get("userDomain"));
                domainKeystoneV3Parameters.setDomainName((String) source.get("domainName"));
                v3Parameters.setDomain(domainKeystoneV3Parameters);
            } else {
                throw new InvalidParameterException("Unalbe to decide Keystone V3 subtype!");
            }
            parameters.setKeystoneV3Parameters(v3Parameters);
        }
        return parameters;
    }

}
