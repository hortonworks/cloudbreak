package com.sequenceiq.freeipa.converter.authentication;

import org.springframework.stereotype.Component;

import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.authentication.StackAuthenticationV4Request;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.freeipa.entity.StackAuthentication;

@Component
public class StackAuthenticationV4RequestToStackAuthenticationConverter
        extends AbstractConversionServiceAwareConverter<StackAuthenticationV4Request, StackAuthentication> {

    @Override
    public StackAuthentication convert(StackAuthenticationV4Request source) {
        StackAuthentication stackAuthentication = new StackAuthentication();
        stackAuthentication.setLoginUserName(Strings.isNullOrEmpty(source.getLoginUserName()) ? "freeipa" : source.getLoginUserName());
        stackAuthentication.setPublicKey(source.getPublicKey());
        stackAuthentication.setPublicKeyId(source.getPublicKeyId());
        return stackAuthentication;
    }
}
