package com.sequenceiq.distrox.v1.distrox.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.authentication.StackAuthenticationV4Request;
import com.sequenceiq.distrox.api.v1.distrox.model.authentication.DistroXAuthenticationV1Request;

@Component
public class DistroXAuthenticationToStaAuthenticationConverter {

    public StackAuthenticationV4Request convert(DistroXAuthenticationV1Request source) {
        StackAuthenticationV4Request response = new StackAuthenticationV4Request();
        response.setLoginUserName(source.getLoginUserName());
        response.setPublicKey(source.getPublicKey());
        response.setPublicKeyId(source.getPublicKeyId());
        return response;
    }

    public DistroXAuthenticationV1Request convert(StackAuthenticationV4Request source) {
        DistroXAuthenticationV1Request response = new DistroXAuthenticationV1Request();
        response.setLoginUserName(source.getLoginUserName());
        response.setPublicKey(source.getPublicKey());
        response.setPublicKeyId(source.getPublicKeyId());
        return response;
    }
}
