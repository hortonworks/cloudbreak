package com.sequenceiq.it.cloudbreak.newway;

import java.util.function.BiConsumer;
import java.util.function.Function;

import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.cloudbreak.newway.action.Action;
import com.sequenceiq.it.cloudbreak.newway.action.KerberosPostAction;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.v3.KerberosV4Action;

@Prototype
public class Kerberos extends KerberosEntity {

    public Kerberos() {
    }

    public Kerberos(TestContext testContext) {
        super(testContext);
    }

    static Function<IntegrationTestContext, Kerberos> getTestContext(String key) {
        return testContext -> testContext.getContextParam(key, Kerberos.class);
    }

    static Function<IntegrationTestContext, Kerberos> getNew() {
        return testContext -> new Kerberos();
    }

    public static Kerberos request() {
        return new Kerberos();
    }

    public static Kerberos isCreated() {
        Kerberos blueprint = new Kerberos();
        blueprint.setCreationStrategy(KerberosV4Action::createInGiven);
        return blueprint;
    }

    public static ResourceAction post(String key) {
        return new ResourceAction(getTestContext(key), KerberosV4Action::post);
    }

    public static ResourceAction post() {
        return post(KERBEROS);
    }

    public static ResourceAction get(String key) {
        return new ResourceAction(getTestContext(key), KerberosV4Action::get);
    }

    public static ResourceAction get() {
        return get(KERBEROS);
    }

    public static ResourceAction getAll() {
        return new ResourceAction(getNew(), KerberosV4Action::getAll);
    }

    public static ResourceAction delete(String key) {
        return new ResourceAction(getTestContext(key), KerberosV4Action::delete);
    }

    public static ResourceAction delete() {
        return delete(KERBEROS);
    }

    public static Assertion<Kerberos> assertThis(BiConsumer<Kerberos, IntegrationTestContext> check) {
        return new Assertion<>(getTestContext(GherkinTest.RESULT), check);
    }

    public static KerberosEntity getByName(TestContext testContext, KerberosEntity entity, CloudbreakClient cloudbreakClient) {
        entity.setResponse(
                cloudbreakClient.getCloudbreakClient().kerberosConfigV4Endpoint().get(cloudbreakClient.getWorkspaceId(), entity.getName())
        );
        return entity;
    }

    public static Action<KerberosEntity> postV2() {
        return new KerberosPostAction();
    }
}