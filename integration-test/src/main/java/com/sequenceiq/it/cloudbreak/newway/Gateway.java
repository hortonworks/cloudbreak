package com.sequenceiq.it.cloudbreak.newway;

import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;

import com.sequenceiq.cloudbreak.api.endpoint.v4.connector.responses.CloudGatewayV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.connector.responses.PlatformGatewaysV4Response;
import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.cloudbreak.newway.v4.GatewayV4Action;

public class Gateway extends Entity {
    private static final String IPPOOL = "IPPOOL";

    private static final Logger LOGGER = LoggerFactory.getLogger(Gateway.class);

    private PlatformResourceParameters request = new PlatformResourceParameters();

    private PlatformGatewaysV4Response response;

    private Gateway(String id) {
        super(id);
    }

    private Gateway() {
        this(IPPOOL);
    }

    public void setRequest(PlatformResourceParameters request) {
        this.request = request;
    }

    public Map<String, Set<CloudGatewayV4Request>> getResponse() {
        return response.getGateways();
    }

    public PlatformResourceParameters getRequest() {
        return request;
    }

    public void setResponse(PlatformGatewaysV4Response response) {
        this.response = response;
    }

    public Gateway withAvailabilityZone(String availabilityZone) {
        request.setAvailabilityZone(availabilityZone);
        return this;
    }

    public Gateway withCredentialName(String name) {
        request.setCredentialName(name);
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
        return new Action<>(getTestContext(key), GatewayV4Action::get);
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
                for (Map.Entry<String, Set<CloudGatewayV4Request>> elem : gateway.getResponse().entrySet()) {
                    for (Object response : elem.getValue()) {
                        CloudGatewayV4Request gatewayJson = (CloudGatewayV4Request) response;
                        Assert.assertFalse(gatewayJson.getName().isEmpty());
                    }
                }
            }
        });
    }
}