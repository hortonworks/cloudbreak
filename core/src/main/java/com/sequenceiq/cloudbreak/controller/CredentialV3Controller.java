package com.sequenceiq.cloudbreak.controller;

import java.util.Map;
import java.util.Set;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.stereotype.Controller;

import com.sequenceiq.cloudbreak.api.endpoint.v3.CredentialV3Endpoint;
import com.sequenceiq.cloudbreak.api.model.CredentialRequest;
import com.sequenceiq.cloudbreak.api.model.CredentialResponse;

@Controller
@Transactional(TxType.NEVER)
public class CredentialV3Controller implements CredentialV3Endpoint {

    @Override
    public Set<CredentialResponse> listByOrganization(Long organizationId) {
        return null;
    }

    @Override
    public CredentialResponse getByNameInOrganization(Long organizationId, String name) {
        return null;
    }

    @Override
    public CredentialResponse createInOrganization(Long organizationId, CredentialRequest request) {
        return null;
    }

    @Override
    public CredentialResponse deleteInOrganization(Long organizationId, String name) {
        return null;
    }

    @Override
    public CredentialResponse putInOrganization(Long organizationId, CredentialRequest credentialRequest) {
        return null;
    }

    @Override
    public Map<String, String> interactiveLogin(Long organizationId, CredentialRequest credentialRequest) {
        return null;
    }
}
