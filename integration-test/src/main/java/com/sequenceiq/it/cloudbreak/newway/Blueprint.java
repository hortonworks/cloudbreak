package com.sequenceiq.it.cloudbreak.newway;

import java.util.function.BiConsumer;
import java.util.function.Function;

import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.cloudbreak.newway.action.Action;
import com.sequenceiq.it.cloudbreak.newway.action.BlueprintPostAction;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.v4.BlueprintV4Action;

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
        blueprint.setCreationStrategy(BlueprintV4Action::createInGiven);
        return blueprint;
    }

    public static ResourceAction post(String key) {
        return new ResourceAction(getTestContext(key), BlueprintV4Action::post);
    }

    public static ResourceAction post() {
        return post(BLUEPRINT);
    }

    public static ResourceAction get(String key) {
        return new ResourceAction(getTestContext(key), BlueprintV4Action::get);
    }

    public static ResourceAction get() {
        return get(BLUEPRINT);
    }

    public static ResourceAction getAll() {
        return new ResourceAction(getNew(), BlueprintV4Action::getAll);
    }

    public static ResourceAction delete(String key) {
        return new ResourceAction(getTestContext(key), BlueprintV4Action::delete);
    }

    public static ResourceAction delete() {
        return delete(BLUEPRINT);
    }

    public static Assertion<Blueprint> assertThis(BiConsumer<Blueprint, IntegrationTestContext> check) {
        return new Assertion<>(getTestContext(GherkinTest.RESULT), check);
    }

    public static BlueprintEntity getByName(TestContext testContext, BlueprintEntity entity, CloudbreakClient cloudbreakClient) {
        entity.setResponse(
                cloudbreakClient.getCloudbreakClient().blueprintV4Endpoint().get(cloudbreakClient.getWorkspaceId(), entity.getName())
        );
        return entity;
    }

    public static Action<BlueprintEntity> postV2() {
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