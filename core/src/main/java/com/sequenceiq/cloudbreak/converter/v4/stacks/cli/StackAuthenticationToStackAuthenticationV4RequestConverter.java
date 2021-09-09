package com.sequenceiq.cloudbreak.converter.v4.stacks.cli;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.authentication.StackAuthenticationV4Request;
import com.sequenceiq.cloudbreak.domain.StackAuthentication;

@Component
public class StackAuthenticationToStackAuthenticationV4RequestConverter {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackAuthenticationToStackAuthenticationV4RequestConverter.class);

    public StackAuthenticationV4Request convert(StackAuthentication source) {
        StackAuthenticationV4Request stackAuthenticationRequest = new StackAuthenticationV4Request();
        stackAuthenticationRequest.setLoginUserName(null);
        stackAuthenticationRequest.setPublicKey(source.getPublicKey());
        stackAuthenticationRequest.setPublicKeyId(source.getPublicKeyId());
        return stackAuthenticationRequest;
    }

}
