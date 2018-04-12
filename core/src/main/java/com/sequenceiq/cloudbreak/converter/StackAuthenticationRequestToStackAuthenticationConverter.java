package com.sequenceiq.cloudbreak.converter;

import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.api.model.StackAuthenticationRequest;
import com.sequenceiq.cloudbreak.domain.StackAuthentication;
import org.springframework.stereotype.Component;

@Component
public class StackAuthenticationRequestToStackAuthenticationConverter
        extends AbstractConversionServiceAwareConverter<StackAuthenticationRequest, StackAuthentication> {
    @Override
    public StackAuthentication convert(StackAuthenticationRequest source) {
        StackAuthentication stackAuthentication = new StackAuthentication();
        stackAuthentication.setLoginUserName(Strings.isNullOrEmpty(source.getLoginUserName()) ? "cloudbreak" : source.getLoginUserName());
        stackAuthentication.setPublicKey(source.getPublicKey());
        stackAuthentication.setPublicKeyId(source.getPublicKeyId());
        return stackAuthentication;
    }
}
