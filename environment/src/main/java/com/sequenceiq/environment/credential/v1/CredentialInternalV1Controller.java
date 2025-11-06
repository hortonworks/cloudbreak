package com.sequenceiq.environment.credential.v1;

import static com.sequenceiq.common.model.CredentialType.ENVIRONMENT;

import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.InternalOnly;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.security.internal.ResourceCrn;
import com.sequenceiq.environment.api.v1.credential.endpoint.CredentialInternalEndpoint;
import com.sequenceiq.environment.api.v1.credential.model.response.CredentialResponse;
import com.sequenceiq.environment.credential.service.CredentialService;
import com.sequenceiq.environment.credential.v1.converter.CredentialToCredentialV1ResponseConverter;
import com.sequenceiq.notification.WebSocketNotificationController;

@Controller
public class CredentialInternalV1Controller extends WebSocketNotificationController implements CredentialInternalEndpoint {

    private final CredentialService credentialService;

    private final CredentialToCredentialV1ResponseConverter credentialConverter;

    public CredentialInternalV1Controller(
            CredentialService credentialService,
            CredentialToCredentialV1ResponseConverter credentialConverter) {
        this.credentialService = credentialService;
        this.credentialConverter = credentialConverter;
    }

    @Override
    @InternalOnly
    public CredentialResponse getByResourceCrn(@ResourceCrn String credentialCrn) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        return credentialConverter.convert(credentialService.getByCrnForAccountId(credentialCrn, accountId, ENVIRONMENT, true));
    }
}
