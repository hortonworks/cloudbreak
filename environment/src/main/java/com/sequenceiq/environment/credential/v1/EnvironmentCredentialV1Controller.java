package com.sequenceiq.environment.credential.v1;

import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.AuthorizationResource;
import com.sequenceiq.authorization.annotation.CheckPermissionByAccount;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.authorization.resource.AuthorizationResourceType;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.security.internal.InternalReady;
import com.sequenceiq.cloudbreak.auth.security.internal.ResourceCrn;
import com.sequenceiq.environment.api.v1.credential.endpoint.EnvironmentCredentialEndpoint;
import com.sequenceiq.environment.api.v1.credential.model.response.CredentialResponse;
import com.sequenceiq.environment.credential.domain.Credential;
import com.sequenceiq.environment.credential.service.CredentialService;
import com.sequenceiq.environment.credential.v1.converter.CredentialToCredentialV1ResponseConverter;
import com.sequenceiq.notification.NotificationController;

@Controller
@InternalReady
@AuthorizationResource(type = AuthorizationResourceType.ENVIRONMENT)
public class EnvironmentCredentialV1Controller extends NotificationController implements EnvironmentCredentialEndpoint {

    private final CredentialService credentialService;

    private final CredentialToCredentialV1ResponseConverter credentialConverter;

    public EnvironmentCredentialV1Controller(
            CredentialService credentialService,
            CredentialToCredentialV1ResponseConverter credentialConverter) {
        this.credentialService = credentialService;
        this.credentialConverter = credentialConverter;
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.READ)
    public CredentialResponse getByEnvironmentCrn(@ResourceCrn String environmentCrn) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        Credential credential = credentialService.getByEnvironmentCrnAndAccountId(environmentCrn, accountId);
        return credentialConverter.convert(credential);
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.READ)
    public CredentialResponse getByEnvironmentName(String environmentName) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        Credential credential = credentialService.getByEnvironmentNameAndAccountId(environmentName, accountId);
        return credentialConverter.convert(credential);
    }
}
