package com.sequenceiq.mock.salt.response;

import org.springframework.stereotype.Component;

import com.sequenceiq.mock.salt.SaltResponse;

@Component
public class KeyDeleteSaltResponse implements SaltResponse {

    @Override
    public Object run(String mockUuid, String body) throws Exception {
        return "";
    }

    @Override
    public String cmd() {
        return "key.delete";
    }
}
