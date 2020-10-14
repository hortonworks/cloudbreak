package com.sequenceiq.mock.legacy.freeipa;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.mock.legacy.freeipa.response.AbstractFreeIpaResponse;
import com.sequenceiq.mock.legacy.freeipa.response.DummyResponse;

@Component
public class FreeIpaRouteHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaRouteHandler.class);

    @Inject
    private Set<AbstractFreeIpaResponse> responses;

    @Inject
    private DummyResponse dummyResponse;

    private Map<String, AbstractFreeIpaResponse> responseByMethod = new HashMap<>();

    @PostConstruct
    public void init() {
        for (AbstractFreeIpaResponse response : responses) {
            responseByMethod.put(response.method(), response);
        }
    }

    public Object handle(String body) throws Exception {
        String method = JsonUtil.readTree(body).findValue("method").textValue();
        if (StringUtils.isNotBlank(method)) {
            LOGGER.debug("Find response for method: [{}]", method);
            AbstractFreeIpaResponse ipaResponse = responseByMethod.getOrDefault(method, dummyResponse);
            LOGGER.debug("Found response method: [{}]", ipaResponse.method());
            return ipaResponse.handle(body);
        } else {
            LOGGER.warn("No method found for request");
            return dummyResponse.handle(body);
        }
    }

    public void updateResponse(String method, AbstractFreeIpaResponse response) {
        responseByMethod.put(method, response);
    }
}
