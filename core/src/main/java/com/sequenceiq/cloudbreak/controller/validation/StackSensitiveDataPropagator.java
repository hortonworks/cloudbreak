package com.sequenceiq.cloudbreak.controller.validation;

import com.sequenceiq.cloudbreak.api.model.CredentialSourceRequest;
import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.service.decorator.CredentialSourceDecorator;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

@Component
public class StackSensitiveDataPropagator {

    @Inject
    private CredentialSourceDecorator credentialSourceDecorator;

    public Stack propagate(CredentialSourceRequest credentialSourceRequest, Stack stack, IdentityUser user) {
        if (credentialSourceRequest != null) {
            Credential decorate = credentialSourceDecorator.decorate(stack.getCredential(), credentialSourceRequest, user);
            stack.setCredential(decorate);
        }
        return stack;
    }
}
