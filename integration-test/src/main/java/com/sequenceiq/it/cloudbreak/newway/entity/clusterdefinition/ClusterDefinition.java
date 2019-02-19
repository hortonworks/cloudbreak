package com.sequenceiq.it.cloudbreak.newway.entity.clusterdefinition;

import java.util.function.BiConsumer;
import java.util.function.Function;

import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.cloudbreak.newway.Assertion;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.GherkinTest;
import com.sequenceiq.it.cloudbreak.newway.Prototype;
import com.sequenceiq.it.cloudbreak.newway.ResourceAction;
import com.sequenceiq.it.cloudbreak.newway.action.Action;
import com.sequenceiq.it.cloudbreak.newway.action.clusterdefinition.ClusterDefinitionDeleteAction;
import com.sequenceiq.it.cloudbreak.newway.action.clusterdefinition.ClusterDefinitionGetAction;
import com.sequenceiq.it.cloudbreak.newway.action.clusterdefinition.ClusterDefinitionGetListAction;
import com.sequenceiq.it.cloudbreak.newway.action.clusterdefinition.ClusterDefinitionPostAction;
import com.sequenceiq.it.cloudbreak.newway.action.clusterdefinition.ClusterDefinitionRequestAction;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.v4.ClusterDefinitionV4Action;

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
        ClusterDefinition clusterDefinition = new ClusterDefinition();
        clusterDefinition.setCreationStrategy(ClusterDefinitionV4Action::createInGiven);
        return clusterDefinition;
    }

    public static ResourceAction post(String key) {
        return new ResourceAction(getTestContext(key), ClusterDefinitionV4Action::post);
    }

    public static ResourceAction post() {
        return post(CLUSTER_DEFINITION);
    }

    public static ResourceAction get(String key) {
        return new ResourceAction(getTestContext(key), ClusterDefinitionV4Action::get);
    }

    public static ResourceAction get() {
        return get(CLUSTER_DEFINITION);
    }

    public static ResourceAction getAll() {
        return new ResourceAction(getNew(), ClusterDefinitionV4Action::getAll);
    }

    public static ResourceAction delete(String key) {
        return new ResourceAction(getTestContext(key), ClusterDefinitionV4Action::delete);
    }

    public static ResourceAction delete() {
        return delete(CLUSTER_DEFINITION);
    }

    public static Assertion<ClusterDefinition> assertThis(BiConsumer<ClusterDefinition, IntegrationTestContext> check) {
        return new Assertion<>(getTestContext(GherkinTest.RESULT), check);
    }

    public static ClusterDefinitionEntity getByName(TestContext testContext, ClusterDefinitionEntity entity, CloudbreakClient cloudbreakClient) {
        entity.setResponse(
                cloudbreakClient.getCloudbreakClient().clusterDefinitionV4Endpoint().get(cloudbreakClient.getWorkspaceId(), entity.getName())
        );
        return entity;
    }

    public static Action<ClusterDefinitionEntity> postV4() {
        return new ClusterDefinitionPostAction();
    }

    public static Action<ClusterDefinitionEntity> listV4() {
        return new ClusterDefinitionGetListAction();
    }

    public static Action<ClusterDefinitionEntity> getV4() {
        return new ClusterDefinitionGetAction();
    }

    public static Action<ClusterDefinitionEntity> deleteV4() {
        return new ClusterDefinitionDeleteAction();
    }

    public static Action<ClusterDefinitionEntity> requestV4() {
        return new ClusterDefinitionRequestAction();
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
    public ClusterDefinition withClusterDefinition(String clusterDefinition) {
        return (ClusterDefinition) super.withClusterDefinition(clusterDefinition);
    }
}