package com.sequenceiq.cloudbreak.service.secret.domain;

import com.sequenceiq.cloudbreak.app.StaticApplicationContext;
import com.sequenceiq.cloudbreak.service.secret.service.SecretService;

public class SecretProxy extends Secret {

    private static final long serialVersionUID = 1L;

    public SecretProxy(String secret) {
        super(null, secret);
    }

    @Override
    public String getRaw() {
        SecretService secretService = StaticApplicationContext.getBean(SecretService.class);
        return secretService.get(getSecret());
    }

    @Override
    public void cacheEvict() {
        SecretService secretService = StaticApplicationContext.getBean(SecretService.class);
        secretService.cacheEvict(getSecret());
    }
}
