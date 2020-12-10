package com.sequenceiq.mock.salt.response;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.orchestrator.salt.domain.ApplyResponse;
import com.sequenceiq.mock.salt.SaltResponse;

@Component
public class JobsActiveSaltResponse implements SaltResponse {

    @Override
    public Object run(String mockUuid, Map<String, List<String>> params) throws Exception {
        ApplyResponse response = new ApplyResponse();
        response.setResult(new ArrayList<>());
        return response;
    }

    @Override
    public String cmd() {
        return "jobs.active";
    }
}
