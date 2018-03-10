package com.sequenceiq.it.cloudbreak.newway;

import com.sequenceiq.cloudbreak.api.model.PlatformIpPoolsResponse;
import com.sequenceiq.cloudbreak.api.model.PlatformResourceRequestJson;
import com.sequenceiq.it.IntegrationTestContext;

import java.util.function.BiConsumer;
import java.util.function.Function;

public class IpPool extends Entity {
    public static final String IPPOOL = "IPPOOL";

    private PlatformResourceRequestJson request;

    private PlatformIpPoolsResponse response;

    public IpPool(String id) {
        super(id);
    }

    public IpPool() {
        this(IPPOOL);
    }

    public void setRequest(PlatformResourceRequestJson request) {
        this.request = request;
    }

    public PlatformIpPoolsResponse getResponse() {
        return response;
    }

    public PlatformResourceRequestJson getRequest() {
        return request;
    }

    public void setResponse(PlatformIpPoolsResponse response) {
        this.response = response;
    }

    public IpPool withCredentialId(Long id) {
        request.setCredentialId(id);
        return this;
    }

    public IpPool withCredentialName(String name) {
        request.setCredentialName(name);
        return this;
    }

    public IpPool withRegion(String region) {
        request.setRegion(region);
        return this;
    }

    static Function<IntegrationTestContext, IpPool> getTestContext(String key) {
        return (testContext) -> testContext.getContextParam(key, IpPool.class);
    }

    static Function<IntegrationTestContext, IpPool> getNew() {
        return (testContext) -> new IpPool();
    }

    public static IpPool request() {
        return new IpPool();
    }

    public static Action<IpPool> get(String key) {
        return new Action<>(getTestContext(key), RegionAction::getRegionsByCredentialId);
    }

    public static Action<IpPool> get() {
        return get(IPPOOL);
    }

    public static Assertion<IpPool> assertThis(BiConsumer<IpPool, IntegrationTestContext> check) {
        return new Assertion<>(getTestContext(GherkinTest.RESULT), check);
    }
}
