package com.sequenceiq.it.cloudbreak.newway.entity;

import javax.ws.rs.core.Response;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.ConfigStrategy;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.ambari.AmbariV4Request;
import com.sequenceiq.it.cloudbreak.newway.Prototype;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;

@Prototype
public class AmbariEntity extends AbstractCloudbreakEntity<AmbariV4Request, Response, AmbariEntity> {

    public AmbariEntity(TestContext testContex) {
        super(new AmbariV4Request(), testContex);
    }

    public AmbariEntity() {
        super(AmbariEntity.class.getSimpleName().toUpperCase());
    }

    public AmbariEntity valid() {
        return withValidateRepositories(true);
    }

    public AmbariEntity withValidateRepositories(Boolean validateRepositories) {
        getRequest().setValidateRepositories(validateRepositories);
        return this;
    }

    public AmbariEntity withStackRepository(String key) {
        StackRepositoryEntity ambariStack = getTestContext().get(key);
        return withStackRepository(ambariStack);
    }

    public AmbariEntity withStackRepository(StackRepositoryEntity ambariStackDetails) {
        getRequest().setStackRepository(ambariStackDetails.getRequest());
        return this;
    }

    public AmbariEntity withAmbariRepoDetails() {
        AmbariRepositoryV4Entity ambariRepo = getTestContext().get(AmbariRepositoryV4Entity.class);
        return withAmbariRepoDetails(ambariRepo);
    }

    public AmbariEntity withAmbariRepoDetails(String key) {
        AmbariRepositoryV4Entity ambariRepo = getTestContext().get(key);
        return withAmbariRepoDetails(ambariRepo);
    }

    public AmbariEntity withAmbariRepoDetails(AmbariRepositoryV4Entity ambariRepoDetailsJson) {
        getRequest().setRepository(ambariRepoDetailsJson.getRequest());
        return this;
    }

    public AmbariEntity withConfigStrategy(ConfigStrategy configStrategy) {
        getRequest().setConfigStrategy(configStrategy);
        return this;
    }

    public AmbariEntity withAmbariSecurityMasterKey(String ambariSecurityMasterKey) {
        getRequest().setSecurityMasterKey(ambariSecurityMasterKey);
        return this;
    }
}
