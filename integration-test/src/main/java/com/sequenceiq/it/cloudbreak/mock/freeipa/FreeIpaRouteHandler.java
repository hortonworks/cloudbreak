package com.sequenceiq.it.cloudbreak.mock.freeipa;

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

import spark.Request;
import spark.Response;
import spark.Route;

@Component
public class FreeIpaRouteHandler implements Route {

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

    @Override
    public Object handle(Request request, Response response) throws Exception {
        String method = JsonUtil.readTree(request.body()).findValue("method").textValue();
        if (StringUtils.isNotBlank(method)) {
            LOGGER.debug("Find response for method: [{}]", method);
            AbstractFreeIpaResponse ipaResponse = responseByMethod.getOrDefault(method, dummyResponse);
            LOGGER.debug("Found response method: [{}]", ipaResponse.method());
            return ipaResponse.handle(request, response);
        } else {
            LOGGER.warn("No method found for request");
            return dummyResponse.handle(request, response);
        }
    }

    public void updateResponse(String method, AbstractFreeIpaResponse response) {
        responseByMethod.put(method, response);
    }
}
