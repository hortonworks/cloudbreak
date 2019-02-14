package com.sequenceiq.it.cloudbreak.newway;

import java.util.function.BiConsumer;
import java.util.function.Function;

import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.cloudbreak.newway.action.ActionV2;
import com.sequenceiq.it.cloudbreak.newway.action.ClusterDefinitionPostAction;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.v3.BlueprintV3Action;

@Prototype
public class ClusterDefinition extends ClusterDefinitionEntity {

    public ClusterDefinition() {
    }

    public ClusterDefinition(TestContext testContext) {
        super(testContext);
    }

    static Function<IntegrationTestContext, ClusterDefinition> getTestContext(String key) {
        return testContext -> testContext.getContextParam(key, ClusterDefinition.class);
    }

    static Function<IntegrationTestContext, ClusterDefinition> getNew() {
        return testContext -> new ClusterDefinition();
    }

    public static ClusterDefinition request() {
        return new ClusterDefinition();
    }

    public static ClusterDefinition isCreated() {
        ClusterDefinition blueprint = new ClusterDefinition();
        blueprint.setCreationStrategy(BlueprintV3Action::createInGiven);
        return blueprint;
    }

    public static Action<ClusterDefinition> post(String key) {
        return new Action<>(getTestContext(key), BlueprintV3Action::post);
    }

    public static Action<ClusterDefinition> post() {
        return post(CLUSTER_DEFINITION);
    }

    public static Action<ClusterDefinition> get(String key) {
        return new Action<>(getTestContext(key), BlueprintV3Action::get);
    }

    public static Action<ClusterDefinition> get() {
        return get(CLUSTER_DEFINITION);
    }

    public static Action<ClusterDefinition> getAll() {
        return new Action<>(getNew(), BlueprintV3Action::getAll);
    }

    public static Action<ClusterDefinition> delete(String key) {
        return new Action<>(getTestContext(key), BlueprintV3Action::delete);
    }

    public static Action<ClusterDefinition> delete() {
        return delete(CLUSTER_DEFINITION);
    }

    public static Assertion<ClusterDefinition> assertThis(BiConsumer<ClusterDefinition, IntegrationTestContext> check) {
        return new Assertion<>(getTestContext(GherkinTest.RESULT), check);
    }

    public static ClusterDefinitionEntity getByName(TestContext testContext, ClusterDefinitionEntity entity, CloudbreakClient cloudbreakClient) {
        entity.setResponse(
                cloudbreakClient.getCloudbreakClient().blueprintV3Endpoint().getByNameInWorkspace(cloudbreakClient.getWorkspaceId(), entity.getName())
        );
        return entity;
    }

    public static ActionV2<ClusterDefinitionEntity> postV2() {
        return new ClusterDefinitionPostAction();
    }

    @Override
    public ClusterDefinition withName(String name) {
        return (ClusterDefinition) super.withName(name);
    }

    @Override
    public ClusterDefinition withDescription(String description) {
        return (ClusterDefinition) super.withDescription(description);
    }

    @Override
    public ClusterDefinition withUrl(String url) {
        return (ClusterDefinition) super.withUrl(url);
    }

    @Override
    public ClusterDefinition withAmbariBlueprint(String blueprint) {
        return (ClusterDefinition) super.withAmbariBlueprint(blueprint);
    }
}