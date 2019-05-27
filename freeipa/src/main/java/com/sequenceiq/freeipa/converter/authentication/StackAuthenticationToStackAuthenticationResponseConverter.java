package com.sequenceiq.freeipa.converter.authentication;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.security.StackAuthenticationResponse;
import com.sequenceiq.freeipa.entity.StackAuthentication;

@Component
public class StackAuthenticationToStackAuthenticationResponseConverter
        implements Converter<StackAuthentication, StackAuthenticationResponse> {

    @Override
    public StackAuthenticationResponse convert(StackAuthentication source) {
        StackAuthenticationResponse stackAuthenticationResponse = new StackAuthenticationResponse();
        stackAuthenticationResponse.setLoginUserName(source.getLoginUserName());
        stackAuthenticationResponse.setPublicKey(source.getPublicKey());
        stackAuthenticationResponse.setPublicKeyId(source.getPublicKeyId());
        return stackAuthenticationResponse;
    }
}
