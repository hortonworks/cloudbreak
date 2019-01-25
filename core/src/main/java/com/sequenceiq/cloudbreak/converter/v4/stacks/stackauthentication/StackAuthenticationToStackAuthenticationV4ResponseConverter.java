package com.sequenceiq.cloudbreak.converter.v4.stacks.stackauthentication;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.stackauthentication.StackAuthenticationV4Response;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.StackAuthentication;

@Component
public class StackAuthenticationToStackAuthenticationV4ResponseConverter
        extends AbstractConversionServiceAwareConverter<StackAuthentication, StackAuthenticationV4Response> {

    @Override
    public StackAuthenticationV4Response convert(StackAuthentication source) {
        StackAuthenticationV4Response stackAuthenticationResponse = new StackAuthenticationV4Response();
        stackAuthenticationResponse.setLoginUserName(source.getLoginUserName());
        stackAuthenticationResponse.setPublicKey(source.getPublicKey());
        stackAuthenticationResponse.setPublicKeyId(source.getPublicKeyId());
        return stackAuthenticationResponse;
    }
}
