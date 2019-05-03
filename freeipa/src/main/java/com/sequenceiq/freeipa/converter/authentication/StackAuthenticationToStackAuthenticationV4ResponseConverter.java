package com.sequenceiq.freeipa.converter.authentication;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.authentication.StackAuthenticationV4Response;
import com.sequenceiq.freeipa.entity.StackAuthentication;

@Component
public class StackAuthenticationToStackAuthenticationV4ResponseConverter
        implements Converter<StackAuthentication, StackAuthenticationV4Response> {

    @Override
    public StackAuthenticationV4Response convert(StackAuthentication source) {
        StackAuthenticationV4Response stackAuthenticationResponse = new StackAuthenticationV4Response();
        stackAuthenticationResponse.setLoginUserName(source.getLoginUserName());
        stackAuthenticationResponse.setPublicKey(source.getPublicKey());
        stackAuthenticationResponse.setPublicKeyId(source.getPublicKeyId());
        return stackAuthenticationResponse;
    }
}
