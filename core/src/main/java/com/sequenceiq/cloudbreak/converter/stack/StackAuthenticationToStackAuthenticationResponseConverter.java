package com.sequenceiq.cloudbreak.converter.stack;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.stack.StackAuthenticationResponse;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.StackAuthentication;

@Component
public class StackAuthenticationToStackAuthenticationResponseConverter
        extends AbstractConversionServiceAwareConverter<StackAuthentication, StackAuthenticationResponse> {
    @Override
    public StackAuthenticationResponse convert(StackAuthentication source) {
        StackAuthenticationResponse stackAuthenticationResponse = new StackAuthenticationResponse();
        stackAuthenticationResponse.setLoginUserName(source.getLoginUserName());
        stackAuthenticationResponse.setPublicKey(source.getPublicKey());
        stackAuthenticationResponse.setPublicKeyId(source.getPublicKeyId());
        return stackAuthenticationResponse;
    }
}
