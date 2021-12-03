package com.sequenceiq.mock.freeipa;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.CloudVmMetaDataStatus;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.mock.freeipa.response.AbstractFreeIpaResponse;
import com.sequenceiq.mock.freeipa.response.DummyResponse;
import com.sequenceiq.mock.spi.SpiStoreService;

@Component
public class FreeIpaRouteHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaRouteHandler.class);

    @Inject
    private Set<AbstractFreeIpaResponse> responses;

    @Inject
    private DummyResponse dummyResponse;

    @Inject
    private SpiStoreService spiStoreService;

    private Map<String, AbstractFreeIpaResponse> responseByMethod = new HashMap<>();

    @PostConstruct
    public void init() {
        for (AbstractFreeIpaResponse response : responses) {
            responseByMethod.put(response.method(), response);
        }
    }

    public Object handle(String body) throws Exception {
        return handle(null, body);
    }

    public Object handle(String mockUuid, String body) throws Exception {
        String method = JsonUtil.readTree(body).findValue("method").textValue();
        List<CloudVmMetaDataStatus> metadatas = spiStoreService.getMetadata(mockUuid);
        if (StringUtils.isNotBlank(method)) {
            LOGGER.debug("Find response for method: [{}]", method);
            AbstractFreeIpaResponse ipaResponse = responseByMethod.getOrDefault(method, dummyResponse);
            LOGGER.debug("Found response method: [{}]", ipaResponse.method());
            return ipaResponse.handle(metadatas, body);
        } else {
            LOGGER.warn("No method found for request");
            return dummyResponse.handle(metadatas, body);
        }
    }

    public void updateResponse(String method, AbstractFreeIpaResponse response) {
        responseByMethod.put(method, response);
    }
}
