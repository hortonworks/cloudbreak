package com.sequenceiq.it.cloudbreak.newway;

import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;

import com.sequenceiq.cloudbreak.api.model.CloudGatewayJson;
import com.sequenceiq.cloudbreak.api.model.PlatformGatewaysResponse;
import com.sequenceiq.cloudbreak.api.model.PlatformResourceRequestJson;
import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.cloudbreak.newway.v3.GatewayV3Action;

public class Gateway extends Entity {
    private static final String IPPOOL = "IPPOOL";

    private static final Logger LOGGER = LoggerFactory.getLogger(Gateway.class);

    private PlatformResourceRequestJson request = new PlatformResourceRequestJson();

    private PlatformGatewaysResponse response;

    private Gateway(String id) {
        super(id);
    }

    private Gateway() {
        this(IPPOOL);
    }

    public void setRequest(PlatformResourceRequestJson request) {
        this.request = request;
    }

    public Map<String, Set<CloudGatewayJson>> getResponse() {
        return response.getGateways();
    }

    public PlatformResourceRequestJson getRequest() {
        return request;
    }

    public void setResponse(PlatformGatewaysResponse response) {
        this.response = response;
    }

    public Gateway withAvailabilityZone(String availabilityZone) {
        request.setAvailabilityZone(availabilityZone);
        return this;
    }

    public Gateway withCredentialId(Long id) {
        request.setCredentialId(id);
        return this;
    }

    public Gateway withCredentialName(String name) {
        request.setCredentialName(name);
        return this;
    }

    public Gateway withFilter(Map<String, String> filter) {
        request.setFilters(filter);
        return this;
    }

    public Gateway withPlatformVariant(String platformVariant) {
        request.setPlatformVariant(platformVariant);
        return this;
    }

    public Gateway withRegion(String region) {
        request.setRegion(region);
        return this;
    }

    static Function<IntegrationTestContext, Gateway> getTestContext(String key) {
        return testContext -> testContext.getContextParam(key, Gateway.class);
    }

    static Function<IntegrationTestContext, Gateway> getNew() {
        return testContext -> new Gateway();
    }

    public static Gateway request() {
        return new Gateway();
    }

    public static Action<Gateway> get(String key) {
        return new Action<>(getTestContext(key), GatewayV3Action::get);
    }

    public static Action<Gateway> get() {
        return get(IPPOOL);
    }

    public static Assertion<Gateway> assertThis(BiConsumer<Gateway, IntegrationTestContext> check) {
        return new Assertion<>(getTestContext(GherkinTest.RESULT), check);
    }

    public static Assertion<Gateway> assertValidGateways() {
        return assertThis((gateway, t) -> {
            if (gateway.getResponse().isEmpty()) {
                LOGGER.info("No gateways for given provider");
            } else {
                for (Map.Entry<String, Set<CloudGatewayJson>> elem : gateway.getResponse().entrySet()) {
                    for (Object response : elem.getValue()) {
                        CloudGatewayJson gatewayJson = (CloudGatewayJson) response;
                        Assert.assertFalse(gatewayJson.getName().isEmpty());
                    }
                }
            }
        });
    }
}