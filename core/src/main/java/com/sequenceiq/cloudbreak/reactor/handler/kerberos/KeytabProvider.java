package com.sequenceiq.cloudbreak.reactor.handler.kerberos;

import javax.inject.Inject;

import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.freeipa.api.v1.kerberosmgmt.KerberosMgmtV1Endpoint;
import com.sequenceiq.freeipa.api.v1.kerberosmgmt.model.ServiceKeytabRequest;
import com.sequenceiq.freeipa.api.v1.kerberosmgmt.model.ServiceKeytabResponse;

@Component
public class KeytabProvider {

    @Inject
    private CmServiceKeytabRequestFactory cmServiceKeytabRequestFactory;

    @Inject
    private KerberosMgmtV1Endpoint kerberosMgmtV1Endpoint;

    @Retryable(maxAttempts = 5, backoff = @Backoff(delay = 5000))
    public ServiceKeytabResponse getServiceKeytabResponse(Stack stack, GatewayConfig primaryGatewayConfig)
            throws Exception {
        ServiceKeytabRequest request = cmServiceKeytabRequestFactory.create(stack, primaryGatewayConfig);
        return kerberosMgmtV1Endpoint.generateServiceKeytab(request);
    }
}
