package com.sequenceiq.mock.legacy.salt.response;

import org.springframework.stereotype.Component;

import com.sequenceiq.mock.legacy.salt.SaltResponse;

@Component
public class KeyDeleteSaltResponse implements SaltResponse {

    @Override
    public Object run(String body) throws Exception {
        return "";
    }

    @Override
    public String cmd() {
        return "key.delete";
    }
}
