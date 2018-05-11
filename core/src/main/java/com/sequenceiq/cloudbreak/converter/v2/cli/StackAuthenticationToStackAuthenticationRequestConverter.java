package com.sequenceiq.cloudbreak.converter.v2.cli;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.stack.StackAuthenticationRequest;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.StackAuthentication;

@Component
public class StackAuthenticationToStackAuthenticationRequestConverter
        extends AbstractConversionServiceAwareConverter<StackAuthentication, StackAuthenticationRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackAuthenticationToStackAuthenticationRequestConverter.class);

    @Override
    public StackAuthenticationRequest convert(StackAuthentication source) {
        StackAuthenticationRequest stackAuthenticationRequest = new StackAuthenticationRequest();
        stackAuthenticationRequest.setLoginUserName(null);
        stackAuthenticationRequest.setPublicKey(source.getPublicKey());
        stackAuthenticationRequest.setPublicKeyId(source.getPublicKeyId());
        return stackAuthenticationRequest;
    }

}
