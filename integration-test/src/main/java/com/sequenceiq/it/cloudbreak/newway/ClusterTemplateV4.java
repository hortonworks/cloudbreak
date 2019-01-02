package com.sequenceiq.it.cloudbreak.newway;

import java.util.function.BiConsumer;
import java.util.function.Function;

import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.cloudbreak.newway.v4.ClusterTemplateV4Action;

public class ClusterTemplateV4 extends ClusterTemplateV4Entity {

    static Function<IntegrationTestContext, ClusterTemplateV4> getTestContext(String key) {
        return testContext -> testContext.getContextParam(key, ClusterTemplateV4.class);
    }

    static Function<IntegrationTestContext, ClusterTemplateV4> getNew() {
        return testContext -> new ClusterTemplateV4();
    }

    public static ClusterTemplateV4 request() {
        return new ClusterTemplateV4();
    }

    public static ClusterTemplateV4 created() {
        ClusterTemplateV4 clusterTemplateV4 = new ClusterTemplateV4();
        clusterTemplateV4.setCreationStrategy(ClusterTemplateV4Action::createInGiven);
        return clusterTemplateV4;
    }

    public static ResourceAction<ClusterTemplateV4> post(String key) {
        return new ResourceAction<>(getTestContext(key), ClusterTemplateV4Action::post);
    }

    public static ResourceAction<ClusterTemplateV4> post() {
        return post(CLUSTER_TEMPLATE);
    }

    public static ResourceAction<ClusterTemplateV4> get(String key) {
        return new ResourceAction<>(getTestContext(key), ClusterTemplateV4Action::get);
    }

    public static ResourceAction<ClusterTemplateV4> get() {
        return get(CLUSTER_TEMPLATE);
    }

    public static ResourceAction<ClusterTemplateV4> getAll() {
        return new ResourceAction<>(getNew(), ClusterTemplateV4Action::createInGiven);
    }

    public static ResourceAction<ClusterTemplateV4> delete(String key) {
        return new ResourceAction<>(getTestContext(key), ClusterTemplateV4Action::delete);
    }

    public static ResourceAction<ClusterTemplateV4> delete() {
        return delete(CLUSTER_TEMPLATE);
    }

    public static Assertion<ClusterTemplateV4> assertThis(BiConsumer<ClusterTemplateV4, IntegrationTestContext> check) {
        return new Assertion<>(getTestContext(GherkinTest.RESULT), check);
    }

    @Override
    public ClusterTemplateV4 withName(String name) {
        return (ClusterTemplateV4) super.withName(name);
    }

    @Override
    public ClusterTemplateV4 withDescription(String description) {
        return (ClusterTemplateV4) super.withDescription(description);
    }
}
