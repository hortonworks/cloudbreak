package com.sequenceiq.freeipa.converter.authentication;

import org.springframework.stereotype.Component;

import com.google.common.base.Strings;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.security.StackAuthenticationRequest;
import com.sequenceiq.freeipa.entity.StackAuthentication;

@Component
public class StackAuthenticationRequestToStackAuthenticationConverter {

    public StackAuthentication convert(StackAuthenticationRequest source) {
        StackAuthentication stackAuthentication = new StackAuthentication();
        stackAuthentication.setLoginUserName(Strings.isNullOrEmpty(source.getLoginUserName()) ? "freeipa" : source.getLoginUserName());
        stackAuthentication.setPublicKey(source.getPublicKey());
        stackAuthentication.setPublicKeyId(source.getPublicKeyId());
        return stackAuthentication;
    }
}
