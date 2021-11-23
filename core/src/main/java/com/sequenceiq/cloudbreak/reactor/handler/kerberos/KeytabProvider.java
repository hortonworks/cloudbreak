package com.sequenceiq.cloudbreak.reactor.handler.kerberos;

import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.auth.crn.InternalCrnBuilder;
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
            String accountId = getAccountId(stack);
            return ThreadBasedUserCrnProvider.doAsInternalActor(() -> kerberosMgmtV1Endpoint.generateServiceKeytab(request, accountId));
        } catch (WebApplicationException e) {
            String errorMessage = exceptionMessageExtractor.getErrorMessage(e);
            String message = String.format("Failed to generate Keytab with FreeIpa service due to: '%s' ", errorMessage);
            LOGGER.warn(message, e);
            throw new CloudbreakServiceException(message, e);
        }
    }

    private String getAccountId(Stack stack) {
        String accountId;
        if (InternalCrnBuilder.isInternalCrn(ThreadBasedUserCrnProvider.getUserCrn())) {
            LOGGER.debug("Current user is internal, getting account id from requested stack crn");
            accountId = Crn.fromString(stack.getResourceCrn()).getAccountId();
        } else {
            LOGGER.debug("Current user is not internal, getting account id from user crn");
            accountId = ThreadBasedUserCrnProvider.getAccountId();
        }
        return accountId;
    }
}
