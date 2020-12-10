package com.sequenceiq.mock.salt.response;

import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Multimap;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.ApplyResponse;
import com.sequenceiq.mock.salt.SaltResponse;
import com.sequenceiq.mock.salt.SaltStoreService;

@Component
public class GrainsRemoveSaltResponse implements SaltResponse {

    @Inject
    private SaltStoreService saltStoreService;

    @Inject
    private ObjectMapper objectMapper;

    @Override
    public Object run(String mockUuid, Map<String, List<String>> params) throws Exception {
        List<String> args = params.get("arg");
        List<String> targets = params.get("tgt");
        Map<String, JsonNode> hostMap = new HashMap<>();
        if (!CollectionUtils.isEmpty(targets) && !CollectionUtils.isEmpty(args)) {
            Map<String, Multimap<String, String>> grains = saltStoreService.getGrains(mockUuid);
            String key = args.get(0);
            String encoded = args.get(1);
            String value = URLDecoder.decode(encoded, Charset.defaultCharset());
            for (String target : targets) {
                if (grains.containsKey(target)) {
                    Multimap<String, String> grainsForTarget = grains.get(target);
                    grainsForTarget.remove(key, value);
                }
                hostMap.put(target, objectMapper.valueToTree(grains.get(target).entries()));
            }
        }
        return createGrainsModificationResponse(hostMap);
    }

    private Object createGrainsModificationResponse(Map<String, JsonNode> hostMap) {
        ApplyResponse response = new ApplyResponse();
        ArrayList<Map<String, JsonNode>> result = new ArrayList<>();
        result.add(hostMap);
        response.setResult(result);
        return response;
    }

    @Override
    public String cmd() {
        return "grains.remove";
    }
}
