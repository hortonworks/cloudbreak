package com.sequenceiq.it.cloudbreak.newway;

import java.util.function.BiConsumer;
import java.util.function.Function;

import org.testng.Assert;

import com.sequenceiq.cloudbreak.api.model.repositoryconfig.RepoConfigValidationRequest;
import com.sequenceiq.cloudbreak.api.model.repositoryconfig.RepoConfigValidationResponse;
import com.sequenceiq.it.IntegrationTestContext;


public class RepositoryConfigs extends Entity {
    private static final String REPOSITORYCONFIGS = "REPOSITORYCONFIGS";

    private RepoConfigValidationRequest repoConfigValidationRequest;

    private RepoConfigValidationResponse response;

    private RepoConfigValidationRequest request;

    private RepositoryConfigs(String id) {
        super(id);
    }

    private RepositoryConfigs() {
        this(REPOSITORYCONFIGS);
    }

    private RepositoryConfigs(RepoConfigValidationRequest repoConfigValidationRequest) {
        this();
        this.repoConfigValidationRequest = repoConfigValidationRequest;
    }

    static Function<IntegrationTestContext, RepositoryConfigs> getTestContext(String key) {
        return (testContext)->testContext.getContextParam(key, RepositoryConfigs.class);
    }

    public static RepositoryConfigs request() {
        return new RepositoryConfigs();
    }

    public static RepositoryConfigs request(RepoConfigValidationRequest nr) {
        return new RepositoryConfigs(nr);
    }

    public void setResponse(RepoConfigValidationResponse response) {
        this.response = response;
    }

    public RepoConfigValidationResponse getRepoConfigsResponse() {
        return response;
    }

    public void setRequest(RepoConfigValidationRequest request) {
        this.request = request;
    }

    public RepoConfigValidationRequest getRequest() {
        return repoConfigValidationRequest;
    }

    public static Action<RepositoryConfigs> post() {
        return post(REPOSITORYCONFIGS);
    }

    public static Action<RepositoryConfigs> post(String key) {
        return new Action<>(getTestContext(key), RepositoryConfigsAction::post);
    }

    public static Assertion<RepositoryConfigs> assertThis(BiConsumer<RepositoryConfigs, IntegrationTestContext> check) {
        return new Assertion<>(getTestContext(GherkinTest.RESULT), check);
    }

    public static Assertion<RepositoryConfigs> assertAllTrue() {
        return assertThis((repositoryConfigs, t) -> {
            Assert.assertTrue(repositoryConfigs.getRepoConfigsResponse().getAmbariBaseUrl());
            Assert.assertTrue(repositoryConfigs.getRepoConfigsResponse().getAmbariGpgKeyUrl());
            Assert.assertTrue(repositoryConfigs.getRepoConfigsResponse().getMpackUrl());
            Assert.assertTrue(repositoryConfigs.getRepoConfigsResponse().getStackBaseURL());
            Assert.assertTrue(repositoryConfigs.getRepoConfigsResponse().getUtilsBaseURL());
            Assert.assertTrue(repositoryConfigs.getRepoConfigsResponse().getVersionDefinitionFileUrl());
        });
    }

    public static Assertion<RepositoryConfigs> assertAllFalse() {
        return assertThis((repositoryConfigs, t) -> {
            Assert.assertFalse(repositoryConfigs.getRepoConfigsResponse().getAmbariBaseUrl());
            Assert.assertFalse(repositoryConfigs.getRepoConfigsResponse().getAmbariGpgKeyUrl());
            Assert.assertFalse(repositoryConfigs.getRepoConfigsResponse().getMpackUrl());
            Assert.assertFalse(repositoryConfigs.getRepoConfigsResponse().getStackBaseURL());
            Assert.assertFalse(repositoryConfigs.getRepoConfigsResponse().getUtilsBaseURL());
            Assert.assertFalse(repositoryConfigs.getRepoConfigsResponse().getVersionDefinitionFileUrl());
        });
    }

    public static Assertion<RepositoryConfigs> assertAllNull() {
        return assertThis((repositoryConfigs, t) -> {
            Assert.assertNull(repositoryConfigs.getRepoConfigsResponse().getAmbariBaseUrl());
            Assert.assertNull(repositoryConfigs.getRepoConfigsResponse().getAmbariGpgKeyUrl());
            Assert.assertNull(repositoryConfigs.getRepoConfigsResponse().getMpackUrl());
            Assert.assertNull(repositoryConfigs.getRepoConfigsResponse().getStackBaseURL());
            Assert.assertNull(repositoryConfigs.getRepoConfigsResponse().getUtilsBaseURL());
            Assert.assertNull(repositoryConfigs.getRepoConfigsResponse().getVersionDefinitionFileUrl());
        });
    }
}
