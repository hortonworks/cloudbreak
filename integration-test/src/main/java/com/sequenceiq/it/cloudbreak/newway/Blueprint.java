package com.sequenceiq.it.cloudbreak.newway;

import java.util.function.BiConsumer;
import java.util.function.Function;

import com.sequenceiq.it.IntegrationTestContext;

public class Blueprint extends BlueprintEntity {

    static Function<IntegrationTestContext, Blueprint> getTestContext(String key) {
        return (testContext) -> testContext.getContextParam(key, Blueprint.class);
    }

    static Function<IntegrationTestContext, Blueprint> getNew() {
        return (testContext) -> new Blueprint();
    }

    public static Blueprint request() {
        return new Blueprint();
    }

    public static Blueprint isCreated() {
        Blueprint blueprint = new Blueprint();
        blueprint.setCreationStrategy(BlueprintAction::createInGiven);
        return blueprint;
    }

    public static Action<Blueprint> post(String key) {
        return new Action<>(getTestContext(key), BlueprintAction::post);
    }

    public static Action<Blueprint> post() {
        return post(BLUEPRINT);
    }

    public static Action<Blueprint> get(String key) {
        return new Action<>(getTestContext(key), BlueprintAction::get);
    }

    public static Action<Blueprint> get() {
        return get(BLUEPRINT);
    }

    public static Action<Blueprint> getAll() {
        return new Action<>(getNew(), BlueprintAction::getAll);
    }

    public static Action<Blueprint> delete(String key) {
        return new Action<>(getTestContext(key), BlueprintAction::delete);
    }

    public static Action<Blueprint> delete() {
        return delete(BLUEPRINT);
    }

    public static Assertion<Blueprint> assertThis(BiConsumer<Blueprint, IntegrationTestContext> check) {
        return new Assertion<>(getTestContext(GherkinTest.RESULT), check);
    }

    @Override
    public Blueprint withName(String name) {
        return (Blueprint) super.withName(name);
    }

    @Override
    public Blueprint withDescription(String description) {
        return (Blueprint) super.withDescription(description);
    }

    @Override
    public Blueprint withUrl(String url) {
        return (Blueprint) super.withUrl(url);
    }

    @Override
    public Blueprint withAmbariBlueprint(String blueprint) {
        return (Blueprint) super.withAmbariBlueprint(blueprint);
    }
}
