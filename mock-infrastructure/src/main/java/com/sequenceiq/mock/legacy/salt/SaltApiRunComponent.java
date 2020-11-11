package com.sequenceiq.mock.legacy.salt;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.common.collect.Multimap;

@Component
public class SaltApiRunComponent {

    private static final Logger LOGGER = LoggerFactory.getLogger(SaltApiRunComponent.class);

    private final Map<String, Multimap<String, String>> grains = new HashMap<>();

    @Inject
    private List<SaltResponse> saltResponses;

    private Map<String, SaltResponse> saltResponsesMap = new HashMap<>();

    @PostConstruct
    public void setup() {
        saltResponses.forEach(s -> saltResponsesMap.put(s.cmd(), s));
    }

    public Object createSaltApiResponse(String body) throws Exception {
        String fun = getParams(body).get("fun");
        if (fun != null) {
            SaltResponse saltResponse = saltResponsesMap.get(fun);
            if (saltResponse != null) {
                return saltResponse.run(body);

            }
        }
        LOGGER.error("no response for this SALT RUN request: " + body);
        throw new IllegalStateException("no response for this SALT RUN request: " + body);
    }

    public Map<String, String> getParams(String body) {
        String[] split = body.split("&");
        Map<String, String> params = new HashMap<>();
        Arrays.stream(split).forEach(s -> {
            String[] split1 = s.split("=");
            params.put(split1[0], split1[1]);
        });
        return params;
    }
}
