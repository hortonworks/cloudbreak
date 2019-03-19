package com.sequenceiq.it.cloudbreak.newway;

import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;

import org.testng.Assert;

import com.sequenceiq.cloudbreak.api.endpoint.v4.connector.responses.PlatformNetworkV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.connector.responses.PlatformNetworksV4Response;
import com.sequenceiq.it.IntegrationTestContext;

public class Networks extends Entity {
    private static final String NETWORKS = "NETWORKS";

    private final PlatformResourceParameters platformResourceParameters = new PlatformResourceParameters();

    private PlatformNetworksV4Response response;

    private Networks() {
        super(NETWORKS);
    }

    public Networks withAvailabilityZone(String availabilityZone) {
        platformResourceParameters.setAvailabilityZone(availabilityZone);
        return this;
    }

    public Networks withCredentialName(String credName) {
        platformResourceParameters.setCredentialName(credName);
        return this;
    }

    public Networks withPlatformVariant(String platformVariant) {
        platformResourceParameters.setPlatformVariant(platformVariant);
        return this;
    }

    public Networks withRegion(String region) {
        platformResourceParameters.setRegion(region);
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

    public PlatformResourceParameters getRequest() {
        return platformResourceParameters;
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
