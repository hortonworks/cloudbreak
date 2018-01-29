package com.sequenceiq.it.cloudbreak.newway;

import com.sequenceiq.it.IntegrationTestContext;

import java.util.function.BiConsumer;
import java.util.function.Function;

public class Credential extends CredentialEntity {

    public static Function<IntegrationTestContext, Credential> getTestContextCredential(String key) {
        return (testContext)->testContext.getContextParam(key, Credential.class);
    }

    public static Function<IntegrationTestContext, Credential> getTestContextCredential() {
        return getTestContextCredential(CREDENTIAL);
    }

    static Function<IntegrationTestContext, Credential> getNew() {
        return (testContext)->new Credential();
    }

    public static Credential request() {
        return new Credential();
    }

    public static Credential isCreated() {
        Credential credential = new Credential();
        credential.setCreationStrategy(CredentialAction::createInGiven);
        return credential;
    }

    public static Action<Credential> post(String key) {
        return new Action<Credential>(getTestContextCredential(key), CredentialAction::post);
    }

    public static Action<Credential> post() {
        return post(CREDENTIAL);
    }

    public static Action<Credential> get(String key) {
        return new Action<>(getTestContextCredential(key), CredentialAction::get);
    }

    public static Action<Credential> get() {
        return get(CREDENTIAL);
    }

    public static Action<Credential> getAll() {
        return new Action<>(getNew(), CredentialAction::getAll);
    }

    public static Action<Credential> delete(String key) {
        return new Action<>(getTestContextCredential(key), CredentialAction::delete);
    }

    public static Action<Credential> delete() {
        return delete(CREDENTIAL);
    }

    public static Assertion<Credential> assertThis(BiConsumer<Credential, IntegrationTestContext> check) {
        return new Assertion<>(getTestContextCredential(GherkinTest.RESULT), check);
    }
}
