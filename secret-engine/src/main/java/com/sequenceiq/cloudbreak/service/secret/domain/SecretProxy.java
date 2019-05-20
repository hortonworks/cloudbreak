package com.sequenceiq.cloudbreak.service.secret.domain;

import com.sequenceiq.cloudbreak.service.secret.service.SecretService;

public class SecretProxy extends Secret {

    private static final long serialVersionUID = 1L;

    private final transient SecretService secretService;

    public SecretProxy(SecretService secretService, String secret) {
        super(null, secret);
        this.secretService = secretService;
    }

    public String getRaw() {
        return secretService.get(getSecret());
    }
}
