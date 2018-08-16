package com.sequenceiq.cloudbreak.controller;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Controller;

import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.service.credential.CredentialService;
import com.sequenceiq.cloudbreak.api.endpoint.v3.CredentialV3Endpoint;
import com.sequenceiq.cloudbreak.api.model.CredentialRequest;
import com.sequenceiq.cloudbreak.api.model.CredentialResponse;

@Controller
@Transactional(TxType.NEVER)
public class CredentialV3Controller extends NotificationController implements CredentialV3Endpoint {

    @Inject
    private CredentialService credentialService;

    @Inject
    @Named("conversionService")
    private ConversionService conversionService;

    @Override
    public Set<CredentialResponse> listByOrganization(Long organizationId) {
        return credentialService.listAvailablesByOrganizationId(organizationId).stream()
                .map(credential -> conversionService.convert(credential, CredentialResponse.class))
                .collect(Collectors.toSet());
    }

    @Override
    public CredentialResponse getByNameInOrganization(Long organizationId, String name) {
        return conversionService.convert(credentialService.getByNameForOrganizationId(name, organizationId), CredentialResponse.class);
    }

    @Override
    public CredentialResponse createInOrganization(Long organizationId, CredentialRequest request) {
        Credential credential = credentialService.create(conversionService.convert(request, Credential.class), organizationId);
        return conversionService.convert(credential, CredentialResponse.class);
    }

    @Override
    public CredentialResponse deleteInOrganization(Long organizationId, String name) {
        Credential deleted = credentialService.deleteByNameFromOrganization(name, organizationId);
        return conversionService.convert(deleted, CredentialResponse.class);
    }

    @Override
    public CredentialResponse putInOrganization(Long organizationId, CredentialRequest credentialRequest) {
        return conversionService.convert(credentialService.updateByOrganizationId(
                organizationId, conversionService.convert(credentialRequest, Credential.class)), CredentialResponse.class);
    }

    @Override
    public Map<String, String> interactiveLogin(Long organizationId, CredentialRequest credentialRequest) {
        return credentialService.interactiveLogin(organizationId, conversionService.convert(credentialRequest, Credential.class));
    }
}