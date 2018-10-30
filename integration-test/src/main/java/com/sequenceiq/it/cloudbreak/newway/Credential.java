package com.sequenceiq.it.cloudbreak.newway;

import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.cloudbreak.newway.action.ActionV2;
import com.sequenceiq.it.cloudbreak.newway.action.CredentialPostAction;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.v3.CredentialV3Action;

import java.util.function.BiConsumer;
import java.util.function.Function;

@Prototype
public class Credential extends CredentialEntity {

    Credential() {
    }

    public Credential(TestContext testContext) {
        super(testContext);
    }

    public static Function<IntegrationTestContext, Credential> getTestContextCredential(String key) {
        return testContext -> testContext.getContextParam(key, Credential.class);
    }

    public static Function<IntegrationTestContext, Credential> getTestContextCredential() {
        return getTestContextCredential(CREDENTIAL);
    }

    static Function<IntegrationTestContext, Credential> getNew() {
        return testContext -> new Credential();
    }

    public static Credential request() {
        return new Credential();
    }

    public static Credential created() {
        return created(1);
    }

    public static Credential created(int retryQuantity) {
        Credential credential = new Credential();
        credential.setCreationStrategy((testContext, entity) -> CredentialV3Action.createInGiven(testContext, entity, retryQuantity));
        return credential;
    }

    public static Credential deleted(Credential credential) {
        return deleted(credential, 1);
    }

    public static Credential deleted(Credential credential, int retryQuantity) {
        credential.setCreationStrategy((testContext, entity) -> CredentialV3Action.createDeleteInGiven(testContext, entity, retryQuantity));

        return credential;
    }

    public static Action<Credential> post(String key) {
        return new Action<>(getTestContextCredential(key), CredentialV3Action::post);
    }

    public static Action<Credential> post() {
        return post(CREDENTIAL);
    }

    public static Action<Credential> put(String key) {
        return new Action<>(getTestContextCredential(key), CredentialV3Action::put);
    }

    public static Action<Credential> put() {
        return put(CREDENTIAL);
    }

    public static Action<Credential> get(String key) {
        return new Action<>(getTestContextCredential(key), CredentialV3Action::get);
    }

    public static Action<Credential> getWithRetryOnTimeout(int retryQuantity) {
        return getWithRetryOnTimeout(CREDENTIAL, retryQuantity);
    }

    public static Action<Credential> getWithRetryOnTimeout(String key, int retryQuantity) {
        return new Action<>(getTestContextCredential(key), (testContext, entity) -> CredentialV3Action.get(testContext, entity, retryQuantity));
    }

    public static Action<Credential> get() {
        return get(CREDENTIAL);
    }

    public static Action<Credential> getAll() {
        return new Action<>(getNew(), CredentialV3Action::getAll);
    }

    public static Action<Credential> getAllWithRetryOnTimeout(int retryQuantity) {
        return new Action<>(getNew(), (testContext, entity) -> CredentialV3Action.getAll(testContext, entity, retryQuantity));
    }

    public static Action<Credential> delete(String key) {
        return new Action<>(getTestContextCredential(key), CredentialV3Action::delete);
    }

    public static Action<Credential> delete() {
        return delete(CREDENTIAL);
    }

    public static Assertion<Credential> assertThis(BiConsumer<Credential, IntegrationTestContext> check) {
        return new Assertion<>(getTestContextCredential(GherkinTest.RESULT), check);
    }

    public static ActionV2<CredentialEntity> postV2() {
        return new CredentialPostAction();
    }

    public static CredentialEntity getByName(TestContext testContext, CredentialEntity entity, CloudbreakClient cloudbreakClient) {
        entity.setResponse(
                cloudbreakClient.getCloudbreakClient().credentialV3Endpoint().getByNameInWorkspace(cloudbreakClient.getWorkspaceId(), entity.getName())
        );
        return entity;
    }
}
