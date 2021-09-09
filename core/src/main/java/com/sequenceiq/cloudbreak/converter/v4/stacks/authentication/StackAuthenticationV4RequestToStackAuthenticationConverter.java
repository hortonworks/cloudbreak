package com.sequenceiq.cloudbreak.converter.v4.stacks.authentication;

import org.springframework.stereotype.Component;

import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.authentication.StackAuthenticationV4Request;
import com.sequenceiq.cloudbreak.domain.StackAuthentication;

@Component
public class StackAuthenticationV4RequestToStackAuthenticationConverter {

    public StackAuthentication convert(StackAuthenticationV4Request source) {
        StackAuthentication stackAuthentication = new StackAuthentication();
        stackAuthentication.setLoginUserName(Strings.isNullOrEmpty(source.getLoginUserName()) ? "cloudbreak" : source.getLoginUserName());
        stackAuthentication.setPublicKey(source.getPublicKey());
        stackAuthentication.setPublicKeyId(source.getPublicKeyId());
        return stackAuthentication;
    }
}
