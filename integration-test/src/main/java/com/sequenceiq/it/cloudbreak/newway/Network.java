package com.sequenceiq.it.cloudbreak.newway;

import java.util.function.BiConsumer;
import java.util.function.Function;

import com.sequenceiq.it.IntegrationTestContext;

public class Network extends NetworkEntity {

    public static Function<IntegrationTestContext, Network> getTestContextNetwork(String key) {
        return (testContext) -> testContext.getContextParam(key, Network.class);
    }

    public static Function<IntegrationTestContext, Network> getTestContextNetwork() {
        return getTestContextNetwork(NETWORK);
    }

    static Function<IntegrationTestContext, Network> getNew() {
        return (testContext) -> new Network();
    }

    public static Network request() {
        return new Network();
    }

    public static Network isCreated() {
        Network network = new Network();
        network.setCreationStrategy(NetworkAction::createInGiven);
        return network;
    }

    public static Network isDeleted(Network network) {
        network.setCreationStrategy(NetworkAction::createDeleteInGiven);
        return network;
    }

    public static Action<Network> post(String key) {
        return new Action<>(getTestContextNetwork(key), NetworkAction::post);
    }

    public static Action<Network> post() {
        return post(NETWORK);
    }

    public static Action<Network> get(String key) {
        return new Action<>(getTestContextNetwork(key), NetworkAction::get);
    }

    public static Action<Network> get() {
        return get(NETWORK);
    }

    public static Action<Network> getAll() {
        return new Action<>(getNew(), NetworkAction::getAll);
    }

    public static Action<Network> delete(String key) {
        return new Action<>(getTestContextNetwork(key), NetworkAction::delete);
    }

    public static Action<Network> delete() {
        return delete(NETWORK);
    }

    public static Assertion<Network> assertThis(BiConsumer<Network, IntegrationTestContext> check) {
        return new Assertion<>(getTestContextNetwork(GherkinTest.RESULT), check);
    }
}
