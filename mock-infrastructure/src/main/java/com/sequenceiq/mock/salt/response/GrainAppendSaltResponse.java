package com.sequenceiq.mock.salt.response;

import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.ApplyResponse;
import com.sequenceiq.mock.salt.SaltResponse;
import com.sequenceiq.mock.salt.SaltStoreService;

@Component
public class GrainAppendSaltResponse implements SaltResponse {

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
                appendGrainValueIfAbsent(grains, target, key, value);
                hostMap.put(target, objectMapper.valueToTree(grains.get(target).values()));
            }
        }
        return createGrainsModificationResponse(hostMap);
    }

    private void appendGrainValueIfAbsent(Map<String, Multimap<String, String>> grains, String target, String key, String value) {
        Multimap<String, String> grainsForTarget = grains.computeIfAbsent(target, t -> ArrayListMultimap.create());
        if (!grainsForTarget.containsEntry(key, value)) {
            grainsForTarget.put(key, value);
        }
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
        return "grains.append";
    }
}
