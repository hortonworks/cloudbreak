package com.sequenceiq.mock.salt.response;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.ApplyResponse;
import com.sequenceiq.mock.salt.SaltResponse;

@Component
public class JobsLookupJidSaltResponse implements SaltResponse {

    @Override
    public Object run(String mockUuid, Map<String, List<String>> params) throws Exception {
        ApplyResponse applyResponse = new ApplyResponse();
        applyResponse.setResult(List.of(Map.of("data", new ObjectMapper().createObjectNode())));
        return applyResponse;
    }

    @Override
    public String cmd() {
        return "jobs.lookup_jid";
    }
}
