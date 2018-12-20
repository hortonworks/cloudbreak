package com.sequenceiq.it.cloudbreak.newway;

import java.util.function.BiConsumer;
import java.util.function.Function;

import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.cloudbreak.newway.action.ActionV2;
import com.sequenceiq.it.cloudbreak.newway.action.KerberosPostAction;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.v3.KerberosV3Action;

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
        blueprint.setCreationStrategy(KerberosV3Action::createInGiven);
        return blueprint;
    }

    public static Action<Kerberos> post(String key) {
        return new Action<>(getTestContext(key), KerberosV3Action::post);
    }

    public static Action<Kerberos> post() {
        return post(KERBEROS);
    }

    public static Action<Kerberos> get(String key) {
        return new Action<>(getTestContext(key), KerberosV3Action::get);
    }

    public static Action<Kerberos> get() {
        return get(KERBEROS);
    }

    public static Action<Kerberos> getAll() {
        return new Action<>(getNew(), KerberosV3Action::getAll);
    }

    public static Action<Kerberos> delete(String key) {
        return new Action<>(getTestContext(key), KerberosV3Action::delete);
    }

    public static Action<Kerberos> delete() {
        return delete(KERBEROS);
    }

    public static Assertion<Kerberos> assertThis(BiConsumer<Kerberos, IntegrationTestContext> check) {
        return new Assertion<>(getTestContext(GherkinTest.RESULT), check);
    }

    public static KerberosEntity getByName(TestContext testContext, KerberosEntity entity, CloudbreakClient cloudbreakClient) {
        entity.setResponse(
                cloudbreakClient.getCloudbreakClient().kerberosConfigV3Endpoint().getByNameInWorkspace(cloudbreakClient.getWorkspaceId(), entity.getName())
        );
        return entity;
    }

    public static ActionV2<KerberosEntity> postV2() {
        return new KerberosPostAction();
    }
}