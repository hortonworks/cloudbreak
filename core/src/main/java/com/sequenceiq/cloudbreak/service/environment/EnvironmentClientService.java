package com.sequenceiq.cloudbreak.service.environment;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.authorization.service.AuthorizationResourceCrnProvider;
import com.sequenceiq.authorization.service.EnvironmentPropertyProvider;
import com.sequenceiq.cloudbreak.common.exception.WebApplicationExceptionMessageExtractor;

@Service
public class EnvironmentClientService implements EnvironmentPropertyProvider, AuthorizationResourceCrnProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentClientService.class);

    @Inject
    private EnvironmentService environmentService;

    @Inject
    private WebApplicationExceptionMessageExtractor webApplicationExceptionMessageExtractor;

    @Override
    public String getResourceCrnByResourceName(String resourceName) {
        return environmentService.getCrnByName(resourceName);
    }
}
