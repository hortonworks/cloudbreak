package com.sequenceiq.mock.salt.response;

import static com.sequenceiq.mock.HostNameUtil.responseFromJsonFile;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.ApplyResponse;
import com.sequenceiq.mock.salt.SaltResponse;

@Component
public class StateHighStateSaltResponse implements SaltResponse {

    @Override
    public Object run(String mockUuid, String body) throws Exception {
        String jsonFile = responseFromJsonFile("saltapi/high_state_response.json");
        return JsonUtil.readValue(jsonFile, ApplyResponse.class);
    }

    @Override
    public String cmd() {
        return "state.highstate";
    }
}
