package com.sequenceiq.it.cloudbreak.newway.entity;

import com.sequenceiq.cloudbreak.api.model.v2.GeneralSettings;
import com.sequenceiq.it.cloudbreak.newway.AbstractCloudbreakEntity;
import com.sequenceiq.it.cloudbreak.newway.CredentialEntity;
import com.sequenceiq.it.cloudbreak.newway.EnvironmentEntity;
import com.sequenceiq.it.cloudbreak.newway.Prototype;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;

@Prototype
public class GeneralSettingsEntity extends AbstractCloudbreakEntity<GeneralSettings, GeneralSettings, GeneralSettingsEntity> {

    protected GeneralSettingsEntity(TestContext testContext) {
        super(new GeneralSettings(), testContext);
    }

    @Override
    public String getName() {
        return getRequest().getName();
    }

    @Override
    public CloudbreakEntity valid() {
        String randomNameForMock = getNameCreator().getRandomNameForMock();
        return withName(randomNameForMock)
                .withCredentialName(getTestContext().get(CredentialEntity.class).getName());
    }

    public GeneralSettingsEntity withName(String name) {
        getRequest().setName(name);
        return this;
    }

    public GeneralSettingsEntity withCredentialName(String credentialName) {
        getRequest().setCredentialName(credentialName);
        return this;
    }

    public GeneralSettingsEntity withEnvironmentName(String environmentName) {
        getRequest().setEnvironmentName(environmentName);
        return this;
    }

    public GeneralSettingsEntity withEnvironmentKey(String environmenKey) {
        EnvironmentEntity environment = getTestContext().get(environmenKey);
        getRequest().setEnvironmentName(environment.getName());
        return this;
    }
}
