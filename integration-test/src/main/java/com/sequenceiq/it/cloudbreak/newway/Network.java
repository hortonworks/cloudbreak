package com.sequenceiq.it.cloudbreak.newway;

import java.util.function.BiConsumer;
import java.util.function.Function;

import com.sequenceiq.it.IntegrationTestContext;

public class Network extends NetworkEntity {

    public static Function<IntegrationTestContext, Network> getTestContextNetwork(String key) {
        return testContext -> testContext.getContextParam(key, Network.class);
    }

    public static Function<IntegrationTestContext, Network> getTestContextNetwork() {
        return getTestContextNetwork(NETWORK);
    }

    static Function<IntegrationTestContext, Network> getNew() {
        return testContext -> new Network();
    }

    public static Network request() {
        return new Network();
    }

    public static Assertion<Network> assertThis(BiConsumer<Network, IntegrationTestContext> check) {
        return new Assertion<>(getTestContextNetwork(GherkinTest.RESULT), check);
    }
}
