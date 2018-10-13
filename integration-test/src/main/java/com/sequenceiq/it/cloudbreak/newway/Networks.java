package com.sequenceiq.it.cloudbreak.newway;

import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;

import org.testng.Assert;

import com.sequenceiq.cloudbreak.api.model.PlatformNetworkResponse;
import com.sequenceiq.cloudbreak.api.model.PlatformNetworksResponse;
import com.sequenceiq.cloudbreak.api.model.PlatformResourceRequestJson;
import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.cloudbreak.newway.v3.NetworksV3Action;

public class Networks extends Entity {
    private static final String NETWORKS = "NETWORKS";

    private final PlatformResourceRequestJson platformResourceRequestJson = new PlatformResourceRequestJson();

    private PlatformNetworksResponse response;

    private Networks() {
        super(NETWORKS);
    }

    public Networks withAvailabilityZone(String availabilityZone) {
        platformResourceRequestJson.setAvailabilityZone(availabilityZone);
        return this;
    }

    public Networks withCredentialId(Long credId) {
        platformResourceRequestJson.setCredentialId(credId);
        return this;
    }

    public Networks withCredentialName(String credName) {
        platformResourceRequestJson.setCredentialName(credName);
        return this;
    }

    public Networks withPlatformVariant(String platformVariant) {
        platformResourceRequestJson.setPlatformVariant(platformVariant);
        return this;
    }

    public Networks withRegion(String region) {
        platformResourceRequestJson.setRegion(region);
        return this;
    }

    public Networks withSetFilters(Map<String, String> filter) {
        platformResourceRequestJson.setFilters(filter);
        return this;
    }

    private static Function<IntegrationTestContext, Networks> getTestContext(String key) {
        return testContext -> testContext.getContextParam(key, Networks.class);
    }

    public static Networks request() {
        return new Networks();
    }

    public void setResponse(PlatformNetworksResponse response) {
        this.response = response;
    }

    private Map<String, Set<PlatformNetworkResponse>> getNetworksResponseWithNetworks() {
        return response.getNetworks();
    }

    public PlatformResourceRequestJson getRequest() {
        return platformResourceRequestJson;
    }

    public static Action<Networks> post() {
        return new Action<>(getTestContext(NETWORKS), NetworksV3Action::getNetworks);
    }

    private static Assertion<Networks> assertThis(BiConsumer<Networks, IntegrationTestContext> check) {
        return new Assertion<>(getTestContext(GherkinTest.RESULT), check);
    }

    public static Assertion<Networks> assertNameNotEmpty() {
        return assertThis((networks, t) -> {
            for (Map.Entry<String, Set<PlatformNetworkResponse>> elem : networks.getNetworksResponseWithNetworks().entrySet()) {
                for (Object response : elem.getValue()) {
                    PlatformNetworkResponse platformNetworksResponse = (PlatformNetworkResponse) response;
                    Assert.assertFalse(platformNetworksResponse.getName().isEmpty());
                }
            }
        });
    }

    public static Assertion<Networks> assertNameEmpty() {
        return assertThis((networks, t) -> {
            for (Map.Entry<String, Set<PlatformNetworkResponse>> elem : networks.getNetworksResponseWithNetworks().entrySet()) {
                for (Object response : elem.getValue()) {
                    PlatformNetworkResponse platformNetworksResponse = (PlatformNetworkResponse) response;
                    Assert.assertTrue(platformNetworksResponse.getName().isEmpty());
                }
            }
        });
    }

    public static Assertion<Networks> assertNetworksEmpty() {
        return assertThis((networks, t) -> {
            for (Map.Entry<String, Set<PlatformNetworkResponse>> elem : networks.getNetworksResponseWithNetworks().entrySet()) {
                Assert.assertTrue("[]".equals(elem.getValue().toString()));
            }
        });
    }
}
