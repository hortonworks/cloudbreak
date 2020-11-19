package com.sequenceiq.mock.salt.response;

import static com.sequenceiq.mock.HostNameUtil.responseFromJsonFile;

import org.springframework.stereotype.Component;

import com.sequenceiq.mock.salt.SaltResponse;

@Component
public class ClouderaAgentUpgradeSaltResponse implements SaltResponse {

    @Override
    public Object run(String mockUuid, String body) throws Exception {
        return responseFromJsonFile("saltapi/show_states.json");
    }

    @Override
    public String cmd() {
        return "cloudera.agent.upgrade";
    }
}
