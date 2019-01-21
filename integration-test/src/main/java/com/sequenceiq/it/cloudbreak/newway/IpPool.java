package com.sequenceiq.it.cloudbreak.newway;

import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;

import com.sequenceiq.cloudbreak.api.endpoint.v4.connector.responses.IpPoolV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.connector.responses.PlatformIpPoolsV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.connector.filters.PlatformResourceV4Filter;
import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.cloudbreak.newway.v3.IpPoolV4Action;

public class IpPool extends Entity {
    private static final Logger LOGGER = LoggerFactory.getLogger(IpPool.class);

    private static final String IPPOOL = "IPPOOL";

    private PlatformResourceV4Filter request = new PlatformResourceV4Filter();

    private PlatformIpPoolsV4Response response;

    private IpPool(String id) {
        super(id);
    }

    private IpPool() {
        this(IPPOOL);
    }

    public void setRequest(PlatformResourceV4Filter request) {
        this.request = request;
    }

    private Map<String, Set<IpPoolV4Response>> getResponseWithIpPools() {
        return response.getIppools();
    }

    public PlatformResourceV4Filter getRequest() {
        return request;
    }

    public void setResponse(PlatformIpPoolsV4Response response) {
        this.response = response;
    }

    public IpPool withAvailabilityZone(String availabilityZone) {
        request.setAvailabilityZone(availabilityZone);
        return this;
    }

    public IpPool withCredentialName(String name) {
        request.setCredentialName(name);
        return this;
    }

    public IpPool withPlatformVariant(String platformVariant) {
        request.setPlatformVariant(platformVariant);
        return this;
    }

    public IpPool withRegion(String region) {
        request.setRegion(region);
        return this;
    }

    private static Function<IntegrationTestContext, IpPool> getTestContext(String key) {
        return testContext -> testContext.getContextParam(key, IpPool.class);
    }

    static Function<IntegrationTestContext, IpPool> getNew() {
        return testContext -> new IpPool();
    }

    public static IpPool request() {
        return new IpPool();
    }

    private static Action<IpPool> get(String key) {
        return new Action<>(getTestContext(key), IpPoolV4Action::get);
    }

    public static Action<IpPool> get() {
        return get(IPPOOL);
    }

    private static Assertion<IpPool> assertThis(BiConsumer<IpPool, IntegrationTestContext> check) {
        return new Assertion<>(getTestContext(GherkinTest.RESULT), check);
    }

    public static Assertion<IpPool> assertValidIpPool() {
        return assertThis((ipPool, t) -> {
            if (ipPool.getResponseWithIpPools().isEmpty()) {
                LOGGER.info("No ipPool for given provider");
            } else {
                for (Map.Entry<String, Set<IpPoolV4Response>> elem : ipPool.getResponseWithIpPools().entrySet()) {
                    for (Object response : elem.getValue()) {
                        IpPoolV4Response ipPoolJson = (IpPoolV4Response) response;
                        Assert.assertFalse(ipPoolJson.getName().isEmpty());
                    }
                }
            }
        });
    }
}
