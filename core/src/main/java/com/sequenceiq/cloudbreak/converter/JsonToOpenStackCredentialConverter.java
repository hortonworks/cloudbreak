package com.sequenceiq.cloudbreak.converter;

import javax.inject.Inject;

import org.jasypt.encryption.pbe.PBEStringCleanablePasswordEncryptor;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.controller.BadRequestException;
import com.sequenceiq.cloudbreak.controller.json.CredentialRequest;
import com.sequenceiq.cloudbreak.controller.validation.OpenStackCredentialParam;
import com.sequenceiq.cloudbreak.domain.OpenStackCredential;

@Component
public class JsonToOpenStackCredentialConverter extends AbstractConversionServiceAwareConverter<CredentialRequest, OpenStackCredential> {

    private static final String SSH_USER = "centos";

    @Inject
    private PBEStringCleanablePasswordEncryptor encryptor;

    @Override
    public OpenStackCredential convert(CredentialRequest source) {
        OpenStackCredential openStackCredential = new OpenStackCredential();
        openStackCredential.setName(source.getName());
        openStackCredential.setDescription(source.getDescription());
        String userName = String.valueOf(source.getParameters().get(OpenStackCredentialParam.USER.getName()));
        openStackCredential.setUserName(encryptor.encrypt(userName));
        String password = String.valueOf(source.getParameters().get(OpenStackCredentialParam.PASSWORD.getName()));
        openStackCredential.setPassword(encryptor.encrypt(password));
        if (!source.getParameters().containsKey(OpenStackCredentialParam.KEYSTONE_VERSION.getName())) {
            openStackCredential.setKeystoneVersion("cb-keystone-v2");
        } else {
            openStackCredential.setKeystoneVersion(String.valueOf(source.getParameters().get(OpenStackCredentialParam.KEYSTONE_VERSION.getName())));
        }
        openStackCredential.setKeystoneAuthScope(String.valueOf(source.getParameters().get(OpenStackCredentialParam.KEYSTONE_AUTH_SCOPE.getName())));
        openStackCredential.setTenantName(String.valueOf(source.getParameters().get(OpenStackCredentialParam.TENANT_NAME.getName())));
        openStackCredential.setUserDomain(String.valueOf(source.getParameters().get(OpenStackCredentialParam.USER_DOMAIN.getName())));
        openStackCredential.setDomainName(String.valueOf(source.getParameters().get(OpenStackCredentialParam.DOMAIN_NAME.getName())));
        openStackCredential.setProjectDomainName(String.valueOf(source.getParameters().get(OpenStackCredentialParam.PROJECT_DOMAIN_NAME.getName())));
        openStackCredential.setProjectName(String.valueOf(source.getParameters().get(OpenStackCredentialParam.PROJECT_NAME.getName())));
        openStackCredential.setEndpoint(String.valueOf(source.getParameters().get(OpenStackCredentialParam.ENDPOINT.getName())));
        openStackCredential.setPublicKey(source.getPublicKey());
        if (source.getLoginUserName() != null) {
            throw new BadRequestException("You can not modify the openstack user!");
        }
        openStackCredential.setLoginUserName(SSH_USER);
        return openStackCredential;
    }
}
