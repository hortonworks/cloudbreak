package com.sequenceiq.datalake.service;

import java.util.Optional;

import org.springframework.stereotype.Service;

import com.sequenceiq.authorization.resource.AuthorizationResourceType;
import com.sequenceiq.authorization.service.ResourcePropertyProvider;

@Service
public class CredentialService implements ResourcePropertyProvider {
    @Override
    public Optional<AuthorizationResourceType> getSupportedAuthorizationResourceType() {
        return Optional.of(AuthorizationResourceType.CREDENTIAL);
    }
}
