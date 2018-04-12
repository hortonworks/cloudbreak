package com.sequenceiq.cloudbreak.converter.v2.cli;

import com.sequenceiq.cloudbreak.api.model.StackAuthenticationRequest;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.StackAuthentication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

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
