package com.sequenceiq.distrox.v1.distrox.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.authentication.StackAuthenticationV4Request;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentAuthenticationResponse;

@Component
public class DistroXAuthenticationToStaAuthenticationConverter {

    public StackAuthenticationV4Request convert(EnvironmentAuthenticationResponse source) {
        StackAuthenticationV4Request response = new StackAuthenticationV4Request();
//        response.setLoginUserName(source.getLoginUserName());
        response.setPublicKey(source.getPublicKey());
        response.setPublicKeyId(source.getPublicKeyId());
        return response;
    }

}
