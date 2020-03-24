package com.sequenceiq.environment.api.v1.proxy.validation;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;


import org.apache.commons.lang3.StringUtils;

import com.sequenceiq.environment.api.v1.proxy.model.request.ProxyRequest;

public class ProxyConfigAuthValidator implements ConstraintValidator<ValidProxyConfigAuthRequest, ProxyRequest> {
    @Override
    public boolean isValid(ProxyRequest proxyRequest, ConstraintValidatorContext constraintValidatorContext) {
        String user = proxyRequest.getUserName();
        String password = proxyRequest.getPassword();
        return StringUtils.isAllEmpty(user, password) || StringUtils.isNoneBlank(user, password);
    }
}
