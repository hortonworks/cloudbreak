package com.sequenceiq.mock.salt.response;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.MinionKeysOnMasterResponse;
import com.sequenceiq.mock.salt.SaltResponse;
import com.sequenceiq.mock.salt.SaltStoreService;

@Component
public class KeyListAllSaltResponse implements SaltResponse {

    @Inject
    private SaltStoreService saltStoreService;

    @Override
    public Object run(String mockUuid, String body) throws Exception {
        MinionKeysOnMasterResponse response = new MinionKeysOnMasterResponse();
        Map<String, JsonNode> result = new HashMap<>();
        ObjectNode data = JsonNodeFactory.instance.objectNode().putObject("data");
        ObjectNode returnNode = data.putObject("return");
        ArrayNode minionsNode = returnNode.putArray("minions");
        saltStoreService.getMinions(mockUuid).forEach(minion -> {
            minionsNode.add(minion.getId());
        });
        result.put("data", data);
        response.setResult(List.of(result));
        return response;
    }

    @Override
    public String cmd() {
        return "key.list_all";
    }
}
