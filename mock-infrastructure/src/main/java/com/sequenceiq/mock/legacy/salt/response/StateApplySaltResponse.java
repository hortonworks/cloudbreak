package com.sequenceiq.mock.legacy.salt.response;

import static com.sequenceiq.mock.legacy.service.HostNameUtil.responseFromJsonFile;

import org.springframework.stereotype.Component;

import com.sequenceiq.mock.legacy.salt.SaltResponse;

@Component
public class StateApplySaltResponse implements SaltResponse {

    @Override
    public Object run(String body) throws Exception {
        return responseFromJsonFile("saltapi/state_apply_response.json");
    }

    @Override
    public String cmd() {
        return "state.apply";
    }
}
