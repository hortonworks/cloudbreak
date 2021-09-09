package com.sequenceiq.cloudbreak.converter.v4.stacks.authentication;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.authentication.StackAuthenticationV4Response;
import com.sequenceiq.cloudbreak.domain.StackAuthentication;

@Component
public class StackAuthenticationToStackAuthenticationV4ResponseConverter {

    public StackAuthenticationV4Response convert(StackAuthentication source) {
        StackAuthenticationV4Response stackAuthenticationResponse = new StackAuthenticationV4Response();
        stackAuthenticationResponse.setLoginUserName(source.getLoginUserName());
        stackAuthenticationResponse.setPublicKey(source.getPublicKey());
        stackAuthenticationResponse.setPublicKeyId(source.getPublicKeyId());
        return stackAuthenticationResponse;
    }
}
