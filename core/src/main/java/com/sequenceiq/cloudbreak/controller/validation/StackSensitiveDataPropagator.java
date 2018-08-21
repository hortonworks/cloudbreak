package com.sequenceiq.cloudbreak.controller.validation;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.CredentialSourceRequest;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.service.decorator.CredentialSourceDecorator;

@Component
public class StackSensitiveDataPropagator {

    @Inject
    private CredentialSourceDecorator credentialSourceDecorator;

    public Stack propagate(CredentialSourceRequest credentialSourceRequest, Stack stack) {
        if (credentialSourceRequest != null) {
            Credential decorate = credentialSourceDecorator.decorate(stack.getCredential(), credentialSourceRequest, stack.getOrganization());
            stack.setCredential(decorate);
        }
        return stack;
    }
}
