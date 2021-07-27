package com.sequenceiq.environment.environment.v1;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.InternalOnly;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.security.internal.TenantAwareParam;
import com.sequenceiq.environment.api.v1.environment.endpoint.EnvironmentInternalEndpoint;
import com.sequenceiq.environment.api.v1.environment.model.response.PolicyValidationErrorResponses;
import com.sequenceiq.environment.credential.service.CredentialService;
import com.sequenceiq.notification.NotificationController;

@Controller
public class EnvironmentInternalV1Controller extends NotificationController implements EnvironmentInternalEndpoint {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentInternalV1Controller.class);

    private final CredentialService credentialService;

    public EnvironmentInternalV1Controller(
            CredentialService credentialService) {
        this.credentialService = credentialService;
    }

    @Override
    @InternalOnly
    public PolicyValidationErrorResponses policyValidationByEnvironmentCrn(@TenantAwareParam String crn, List<String> services) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        return credentialService.validatePolicy(accountId, crn, services);
    }
}
