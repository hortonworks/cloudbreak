package com.sequenceiq.cloudbreak.reactor.handler.kerberos;

import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.exception.WebApplicationExceptionMessageExtractor;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.freeipa.api.v1.kerberosmgmt.KerberosMgmtV1Endpoint;
import com.sequenceiq.freeipa.api.v1.kerberosmgmt.model.ServiceKeytabRequest;
import com.sequenceiq.freeipa.api.v1.kerberosmgmt.model.ServiceKeytabResponse;

@Component
public class KeytabProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(KeytabProvider.class);

    @Inject
    private CmServiceKeytabRequestFactory cmServiceKeytabRequestFactory;

    @Inject
    private KerberosMgmtV1Endpoint kerberosMgmtV1Endpoint;

    @Inject
    private WebApplicationExceptionMessageExtractor exceptionMessageExtractor;

    public ServiceKeytabResponse getServiceKeytabResponse(Stack stack, GatewayConfig primaryGatewayConfig) {
        ServiceKeytabRequest request = cmServiceKeytabRequestFactory.create(stack, primaryGatewayConfig);
        try {
            return kerberosMgmtV1Endpoint.generateServiceKeytab(request);
        } catch (WebApplicationException e) {
            String errorMessage = exceptionMessageExtractor.getErrorMessage(e);
            String message = String.format("Failed to get Keytab from FreeIpa service due to: '%s' ", errorMessage);
            LOGGER.warn(message, e);
            throw new CloudbreakServiceException(message, e);
        }
    }
}
