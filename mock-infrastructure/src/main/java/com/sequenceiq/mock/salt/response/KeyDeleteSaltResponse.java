package com.sequenceiq.mock.salt.response;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.sequenceiq.mock.salt.SaltResponse;

@Component
public class KeyDeleteSaltResponse implements SaltResponse {

    @Override
    public Object run(String mockUuid, Map<String, List<String>> params) throws Exception {
        return "";
    }

    @Override
    public String cmd() {
        return "key.delete";
    }
}
