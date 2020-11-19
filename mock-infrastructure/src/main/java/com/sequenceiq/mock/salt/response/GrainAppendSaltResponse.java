package com.sequenceiq.mock.salt.response;

import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.ApplyResponse;
import com.sequenceiq.mock.salt.SaltResponse;
import com.sequenceiq.mock.salt.SaltStoreService;

@Component
public class GrainAppendSaltResponse implements SaltResponse {

    public static final int GRAIN_VALUE_GROUP = 4;

    @Inject
    private SaltStoreService saltStoreService;

    @Inject
    private ObjectMapper objectMapper;

    @Override
    public Object run(String mockUuid, String body) throws Exception {
        Matcher targetMatcher = Pattern.compile(".*(tgt=([^&]+)).*").matcher(body);
        Matcher argMatcher = Pattern.compile(".*(arg=([^&]+)).*(arg=([^&]+)).*").matcher(body);
        Map<String, JsonNode> hostMap = new HashMap<>();
        if (targetMatcher.matches() && argMatcher.matches()) {
            Map<String, Multimap<String, String>> grains = saltStoreService.getGrains(mockUuid);
            String[] targets = targetMatcher.group(2).split("%2C");
            String key = argMatcher.group(2);
            String decoded = argMatcher.group(GRAIN_VALUE_GROUP);
            String value = URLDecoder.decode(decoded, Charset.defaultCharset());
            for (String target : targets) {
                if (!grains.containsKey(target)) {
                    Multimap<String, String> grainsForTarget = ArrayListMultimap.create();
                    grainsForTarget.put(key, value);
                    grains.put(target, grainsForTarget);
                } else {
                    Multimap<String, String> grainsForTarget = grains.get(target);
                    grainsForTarget.put(key, value);
                }
                hostMap.put(target, objectMapper.valueToTree(grains.get(target).values()));
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
        return "grains.append";
    }
}
