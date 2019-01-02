package com.sequenceiq.it.cloudbreak.newway.entity;

import java.util.Set;

import com.sequenceiq.cloudbreak.api.endpoint.v4.environment.responses.DetailedEnvironmentV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.environment.responses.SimpleEnvironmentV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.environment.EnvironmentSettingsV4Request;
import com.sequenceiq.it.cloudbreak.newway.AbstractCloudbreakEntity;
import com.sequenceiq.it.cloudbreak.newway.CredentialEntity;
import com.sequenceiq.it.cloudbreak.newway.Prototype;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;

@Prototype
public class EnvironmentSettingsV4Entity extends AbstractCloudbreakEntity<EnvironmentSettingsV4Request, DetailedEnvironmentV4Response,
        EnvironmentSettingsV4Entity> {

    public static final String ENVIRONMENT = "ENVIRONMENT";

    private Set<SimpleEnvironmentV4Response> response;

    private SimpleEnvironmentV4Response simpleResponse;

    public EnvironmentSettingsV4Entity(TestContext testContext) {
        super(new EnvironmentSettingsV4Request(), testContext);
    }

    public EnvironmentSettingsV4Entity() {
        super(ENVIRONMENT);
    }

    public EnvironmentSettingsV4Entity(EnvironmentSettingsV4Request environmentV4Request, TestContext testContext) {
        super(environmentV4Request, testContext);
    }

    @Override
    public String getName() {
        return getRequest().getName();
    }

    @Override
    public EnvironmentSettingsV4Entity valid() {
        CredentialEntity credentialEntity = getTestContext().get(CredentialEntity.class);
        if (credentialEntity == null) {
            throw new IllegalArgumentException("Credential is mandatory for EnvironmentSettings");
        }
        return withName(getNameCreator().getRandomNameForMock())
                .withCredentialName(credentialEntity.getName());
    }

    public EnvironmentSettingsV4Entity withName(String name) {
        getRequest().setName(name);
        setName(name);
        return this;
    }

    public EnvironmentSettingsV4Entity withCredentialName(String name) {
        getRequest().setCredentialName(name);
        return this;
    }
}