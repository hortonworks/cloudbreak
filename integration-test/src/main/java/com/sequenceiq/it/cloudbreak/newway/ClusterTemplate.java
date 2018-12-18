package com.sequenceiq.it.cloudbreak.newway;

import java.util.function.BiConsumer;
import java.util.function.Function;

import com.sequenceiq.it.IntegrationTestContext;

public class ClusterTemplate extends ClusterTemplateEntity {

    static Function<IntegrationTestContext, ClusterTemplate> getTestContext(String key) {
        return testContext -> testContext.getContextParam(key, ClusterTemplate.class);
    }

    static Function<IntegrationTestContext, ClusterTemplate> getNew() {
        return testContext -> new ClusterTemplate();
    }

    public static ClusterTemplate request() {
        return new ClusterTemplate();
    }

    public static ClusterTemplate created() {
        ClusterTemplate clusterTemplate = new ClusterTemplate();
        clusterTemplate.setCreationStrategy(ClusterTemplateAction::createInGiven);
        return clusterTemplate;
    }

    public static Action<ClusterTemplate> post(String key) {
        return new Action<>(getTestContext(key), ClusterTemplateAction::post);
    }

    public static Action<ClusterTemplate> post() {
        return post(CLUSTER_TEMPLATE);
    }

    public static Action<ClusterTemplate> get(String key) {
        return new Action<>(getTestContext(key), ClusterTemplateAction::get);
    }

    public static Action<ClusterTemplate> get() {
        return get(CLUSTER_TEMPLATE);
    }

    public static Action<ClusterTemplate> getAll() {
        return new Action<>(getNew(), ClusterTemplateAction::getAll);
    }

    public static Action<ClusterTemplate> delete(String key) {
        return new Action<>(getTestContext(key), ClusterTemplateAction::delete);
    }

    public static Action<ClusterTemplate> delete() {
        return delete(CLUSTER_TEMPLATE);
    }

    public static Assertion<ClusterTemplate> assertThis(BiConsumer<ClusterTemplate, IntegrationTestContext> check) {
        return new Assertion<>(getTestContext(GherkinTest.RESULT), check);
    }

    @Override
    public ClusterTemplate withName(String name) {
        return (ClusterTemplate) super.withName(name);
    }

    @Override
    public ClusterTemplate withDescription(String description) {
        return (ClusterTemplate) super.withDescription(description);
    }
}
