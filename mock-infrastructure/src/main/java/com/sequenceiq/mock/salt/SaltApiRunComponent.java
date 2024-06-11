package com.sequenceiq.mock.salt;

import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;

import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.mock.service.FailureService;

@Component
public class SaltApiRunComponent {

    private static final Logger LOGGER = LoggerFactory.getLogger(SaltApiRunComponent.class);

    @Inject
    private List<SaltResponse> saltResponses;

    @Inject
    private SaltStoreService saltStoreService;

    @Inject
    private FailureService failureService;

    private final Map<String, SaltResponse> saltResponsesMap = new ConcurrentHashMap<>();

    @PostConstruct
    public void setup() {
        saltResponses.forEach(s -> saltResponsesMap.put(s.cmd(), s));
    }

    public Object createSaltApiResponse(String mockUuid, String body) throws Exception {
        Map<String, List<String>> params = getParams(body);
        List<String> fun = params.get("fun");
        List<String> arg = params.get("arg");
        if (CollectionUtils.isNotEmpty(arg) && arg.size() == 1) {
            Set<String> configuredFailures = failureService.getCommandFailures(mockUuid);
            if (CollectionUtils.isNotEmpty(configuredFailures)) {
                if (configuredFailures.contains(arg.get(0))) {
                    String message = String.format("Salt run %s is configured to fail.", arg.get(0));
                    LOGGER.info(message);
                    throw new RuntimeException(message);
                }
            }
        }
        if (!fun.isEmpty()) {
            SaltResponse saltResponse = saltResponsesMap.get(fun.get(0));
            if (saltResponse != null) {
                Object response = saltResponse.run(mockUuid, params);
                storeIfEnabled(mockUuid, params, response);
                return response;
            }
        }
        LOGGER.error("no response for this SALT RUN request: " + body);
        throw new IllegalStateException("no response for this SALT RUN request: " + body);
    }

    private void storeIfEnabled(String mockUuid, Map<String, List<String>> params, Object response) {
        RunResponseDto runResponseDto = new RunResponseDto(mockUuid);
        runResponseDto.setParams(params);
        runResponseDto.setResponse(response);
        saltStoreService.addRunResponse(mockUuid, runResponseDto);
    }

    public Map<String, List<String>> getParams(String body) {
        String[] split = body.split("&");
        Map<String, List<String>> params = new HashMap<>();
        Arrays.stream(split).forEach(s -> {
            String decoded = URLDecoder.decode(s, Charset.defaultCharset());
            String[] split1 = decoded.split("=");
            String value = "";
            if (split1.length == 2) {
                value = split1[1];
            }
            if (split1[0].equals("tgt") || split1[0].equals("match")) {
                List<String> values = Arrays.asList(value.split(","));
                params.put(split1[0], values);
            } else {
                List<String> values = params.computeIfAbsent(split1[0], key -> new ArrayList<>());
                values.add(value);
            }
        });
        return params;
    }
}
