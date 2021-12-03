package com.sequenceiq.mock.salt;

import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.mock.verification.RequestResponseStorageService;

@Component
public class SaltApiRunComponent {

    private static final Logger LOGGER = LoggerFactory.getLogger(SaltApiRunComponent.class);

    @Inject
    private List<SaltResponse> saltResponses;

    @Inject
    private SaltStoreService saltStoreService;

    @Inject
    private RequestResponseStorageService requestResponseStorageService;

    private final Map<String, SaltResponse> saltResponsesMap = new HashMap<>();

    @PostConstruct
    public void setup() {
        saltResponses.forEach(s -> saltResponsesMap.put(s.cmd(), s));
    }

    public Object createSaltApiResponse(String mockUuid, String body) throws Exception {
        Map<String, List<String>> params = getParams(body);
        List<String> fun = params.get("fun");
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
        if (requestResponseStorageService.isEnabledToStore(mockUuid)) {
            RunResponseDto runResponseDto = new RunResponseDto(mockUuid);
            runResponseDto.setParams(params);
            runResponseDto.setResponse(response);
            saltStoreService.addRunResponse(mockUuid, runResponseDto);
        } else {
            LOGGER.debug("Freeipa response store is disabled for {}, {}, response: {}", mockUuid, params, response);
        }
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
            if (split1[0].equals("tgt")) {
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
