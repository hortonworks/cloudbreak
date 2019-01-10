package com.sequenceiq.it.cloudbreak.newway;

import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;

import org.testng.Assert;

import com.sequenceiq.cloudbreak.api.endpoint.v4.connector.responses.PlatformNetworkV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.connector.responses.PlatformNetworksV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.connector.requests.PlatformResourceV4Request;
import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.cloudbreak.newway.v3.NetworksV3Action;

public class Networks extends Entity {
    private static final String NETWORKS = "NETWORKS";

    private final PlatformResourceV4Request platformResourceV4Request = new PlatformResourceV4Request();

    private PlatformNetworksV4Response response;

    private Networks() {
        super(NETWORKS);
    }

    public Networks withAvailabilityZone(String availabilityZone) {
        platformResourceV4Request.setAvailabilityZone(availabilityZone);
        return this;
    }

    public Networks withCredentialId(Long credId) {
        platformResourceV4Request.setCredentialId(credId);
        return this;
    }

    public Networks withCredentialName(String credName) {
        platformResourceV4Request.setCredentialName(credName);
        return this;
    }

    public Networks withPlatformVariant(String platformVariant) {
        platformResourceV4Request.setPlatformVariant(platformVariant);
        return this;
    }

    public Networks withRegion(String region) {
        platformResourceV4Request.setRegion(region);
        return this;
    }

    public Networks withSetFilters(Map<String, String> filter) {
        platformResourceV4Request.setFilters(filter);
        return this;
    }

    private static Function<IntegrationTestContext, Networks> getTestContext(String key) {
        return testContext -> testContext.getContextParam(key, Networks.class);
    }

    public static Networks request() {
        return new Networks();
    }

    public void setResponse(PlatformNetworksV4Response response) {
        this.response = response;
    }

    private Map<String, Set<PlatformNetworkV4Response>> getNetworksResponseWithNetworks() {
        return response.getNetworks();
    }

    public PlatformResourceV4Request getRequest() {
        return platformResourceV4Request;
    }

    public static Action<Networks> post() {
        return new Action<>(getTestContext(NETWORKS), NetworksV3Action::getNetworks);
    }

    private static Assertion<Networks> assertThis(BiConsumer<Networks, IntegrationTestContext> check) {
        return new Assertion<>(getTestContext(GherkinTest.RESULT), check);
    }

    public static Assertion<Networks> assertNameNotEmpty() {
        return assertThis((networks, t) -> {
            for (Map.Entry<String, Set<PlatformNetworkV4Response>> elem : networks.getNetworksResponseWithNetworks().entrySet()) {
                for (Object response : elem.getValue()) {
                    PlatformNetworkV4Response platformNetworksResponse = (PlatformNetworkV4Response) response;
                    Assert.assertFalse(platformNetworksResponse.getName().isEmpty());
                }
            }
        });
    }

    public static Assertion<Networks> assertNameEmpty() {
        return assertThis((networks, t) -> {
            for (Map.Entry<String, Set<PlatformNetworkV4Response>> elem : networks.getNetworksResponseWithNetworks().entrySet()) {
                for (Object response : elem.getValue()) {
                    PlatformNetworkV4Response platformNetworksResponse = (PlatformNetworkV4Response) response;
                    Assert.assertTrue(platformNetworksResponse.getName().isEmpty());
                }
            }
        });
    }

    public static Assertion<Networks> assertNetworksEmpty() {
        return assertThis((networks, t) -> {
            for (Map.Entry<String, Set<PlatformNetworkV4Response>> elem : networks.getNetworksResponseWithNetworks().entrySet()) {
                Assert.assertTrue("[]".equals(elem.getValue().toString()));
            }
        });
    }
}
