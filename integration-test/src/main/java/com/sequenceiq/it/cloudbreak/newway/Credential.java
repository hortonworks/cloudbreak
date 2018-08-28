package com.sequenceiq.it.cloudbreak.newway;

import java.util.function.BiConsumer;
import java.util.function.Function;

import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.cloudbreak.newway.v3.CredentialV3Action;

public class Credential extends CredentialEntity {

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

    public static Credential isCreated() {
        Credential credential = new Credential();
        credential.setCreationStrategy(CredentialV3Action::createInGiven);
        return credential;
    }

    public static Credential isDeleted(Credential credential) {
        credential.setCreationStrategy(CredentialV3Action::createDeleteInGiven);

        return credential;
    }

    public static Action<Credential> post(String key) {
        return new Action<>(getTestContextCredential(key), CredentialV3Action::post);
    }

    public static Action<Credential> post() {
        return post(CREDENTIAL);
    }

    public static Action<Credential> get(String key) {
        return new Action<>(getTestContextCredential(key), CredentialV3Action::get);
    }

    public static Action<Credential> get() {
        return get(CREDENTIAL);
    }

    public static Action<Credential> getAll() {
        return new Action<>(getNew(), CredentialV3Action::getAll);
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
}
