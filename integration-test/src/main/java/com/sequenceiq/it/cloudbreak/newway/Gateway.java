package com.sequenceiq.it.cloudbreak.newway;

import com.sequenceiq.cloudbreak.api.model.PlatformGatewaysResponse;
import com.sequenceiq.cloudbreak.api.model.PlatformResourceRequestJson;
import com.sequenceiq.it.IntegrationTestContext;

import java.util.function.BiConsumer;
import java.util.function.Function;

public class Gateway extends Entity {
    public static final String IPPOOL = "IPPOOL";

    private PlatformResourceRequestJson request;

    private PlatformGatewaysResponse response;

    public Gateway(String id) {
        super(id);
    }

    public Gateway() {
        this(IPPOOL);
    }

    public void setRequest(PlatformResourceRequestJson request) {
        this.request = request;
    }

    public PlatformGatewaysResponse getResponse() {
        return response;
    }

    public PlatformResourceRequestJson getRequest() {
        return request;
    }

    public void setResponse(PlatformGatewaysResponse response) {
        this.response = response;
    }

    public Gateway withCredentialId(Long id) {
        request.setCredentialId(id);
        return this;
    }

    public Gateway withCredentialName(String name) {
        request.setCredentialName(name);
        return this;
    }

    public Gateway withRegion(String region) {
        request.setRegion(region);
        return this;
    }

    static Function<IntegrationTestContext, Gateway> getTestContext(String key) {
        return (testContext) -> testContext.getContextParam(key, Gateway.class);
    }

    static Function<IntegrationTestContext, Gateway> getNew() {
        return (testContext) -> new Gateway();
    }

    public static Gateway request() {
        return new Gateway();
    }

    public static Action<Gateway> get(String key) {
        return new Action<>(getTestContext(key), RegionAction::getRegionsByCredentialId);
    }

    public static Action<Gateway> get() {
        return get(IPPOOL);
    }

    public static Assertion<Gateway> assertThis(BiConsumer<Gateway, IntegrationTestContext> check) {
        return new Assertion<>(getTestContext(GherkinTest.RESULT), check);
    }
}
