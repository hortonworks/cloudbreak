package com.sequenceiq.cloudbreak.controller.validation;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.StackRequest;
import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.service.decorator.CredentialSourceDecorator;

@Component
public class StackSensitiveDataPropagator {

    @Inject
    private CredentialSourceDecorator credentialSourceDecorator;

    public Stack propagate(StackRequest request, Stack stack, IdentityUser user) {
        if (request.getCredentialSource() != null) {
            Credential decorate = credentialSourceDecorator.decorate(stack.getCredential(), request.getCredentialSource(), user);
            stack.setCredential(decorate);
        }
        return stack;
    }
}
