package com.sequenceiq.it.cloudbreak.newway;

import java.util.function.BiConsumer;
import java.util.function.Function;

import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.cloudbreak.newway.action.ActionV2;
import com.sequenceiq.it.cloudbreak.newway.action.BlueprintPostAction;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.v3.BlueprintV3Action;

@Prototype
public class Blueprint extends BlueprintEntity {

    public Blueprint() {
    }

    public Blueprint(TestContext testContext) {
        super(testContext);
    }

    static Function<IntegrationTestContext, Blueprint> getTestContext(String key) {
        return testContext -> testContext.getContextParam(key, Blueprint.class);
    }

    static Function<IntegrationTestContext, Blueprint> getNew() {
        return testContext -> new Blueprint();
    }

    public static Blueprint request() {
        return new Blueprint();
    }

    public static Blueprint isCreated() {
        Blueprint blueprint = new Blueprint();
        blueprint.setCreationStrategy(BlueprintV3Action::createInGiven);
        return blueprint;
    }

    public static Action<Blueprint> post(String key) {
        return new Action<>(getTestContext(key), BlueprintV3Action::post);
    }

    public static Action<Blueprint> post() {
        return post(BLUEPRINT);
    }

    public static Action<Blueprint> get(String key) {
        return new Action<>(getTestContext(key), BlueprintV3Action::get);
    }

    public static Action<Blueprint> get() {
        return get(BLUEPRINT);
    }

    public static Action<Blueprint> getAll() {
        return new Action<>(getNew(), BlueprintV3Action::getAll);
    }

    public static Action<Blueprint> delete(String key) {
        return new Action<>(getTestContext(key), BlueprintV3Action::delete);
    }

    public static Action<Blueprint> delete() {
        return delete(BLUEPRINT);
    }

    public static Assertion<Blueprint> assertThis(BiConsumer<Blueprint, IntegrationTestContext> check) {
        return new Assertion<>(getTestContext(GherkinTest.RESULT), check);
    }

    public static BlueprintEntity getByName(TestContext testContext, BlueprintEntity entity, CloudbreakClient cloudbreakClient) {
        entity.setResponse(
                cloudbreakClient.getCloudbreakClient().blueprintV3Endpoint().getByNameInWorkspace(cloudbreakClient.getWorkspaceId(), entity.getName())
        );
        return entity;
    }

    public static ActionV2<BlueprintEntity> postV2() {
        return new BlueprintPostAction();
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