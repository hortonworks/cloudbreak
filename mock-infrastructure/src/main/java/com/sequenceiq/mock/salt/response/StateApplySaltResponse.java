package com.sequenceiq.mock.salt.response;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.node.TextNode;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.ApplyResponse;
import com.sequenceiq.mock.salt.SaltResponse;

@Component
public class StateApplySaltResponse implements SaltResponse {

    @Override
    public Object run(String mockUuid, Map<String, List<String>> params) throws Exception {
        ApplyResponse response = new ApplyResponse();
        response.setResult(List.of(Map.of("jid", new TextNode("1"))));
        return response;
    }

    @Override
    public String cmd() {
        return "state.apply";
    }
}
