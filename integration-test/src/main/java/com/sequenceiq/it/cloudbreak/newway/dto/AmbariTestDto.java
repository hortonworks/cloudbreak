package com.sequenceiq.it.cloudbreak.newway.dto;

import javax.ws.rs.core.Response;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.ConfigStrategy;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.ambari.AmbariV4Request;
import com.sequenceiq.it.cloudbreak.newway.Prototype;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;

@Prototype
public class AmbariTestDto extends AbstractCloudbreakTestDto<AmbariV4Request, Response, AmbariTestDto> {

    public AmbariTestDto(TestContext testContex) {
        super(new AmbariV4Request(), testContex);
    }

    public AmbariTestDto() {
        super(AmbariTestDto.class.getSimpleName().toUpperCase());
    }

    public AmbariTestDto valid() {
        return withValidateRepositories(true);
    }

    public AmbariTestDto withValidateRepositories(Boolean validateRepositories) {
        getRequest().setValidateRepositories(validateRepositories);
        return this;
    }

    public AmbariTestDto withStackRepository(String key) {
        StackRepositoryTestDto ambariStack = getTestContext().get(key);
        return withStackRepository(ambariStack);
    }

    public AmbariTestDto withStackRepository(StackRepositoryTestDto ambariStackDetails) {
        getRequest().setStackRepository(ambariStackDetails.getRequest());
        return this;
    }

    public AmbariTestDto withAmbariRepoDetails() {
        AmbariRepositoryV4TestDto ambariRepo = getTestContext().get(AmbariRepositoryV4TestDto.class);
        return withAmbariRepoDetails(ambariRepo);
    }

    public AmbariTestDto withAmbariRepoDetails(String key) {
        AmbariRepositoryV4TestDto ambariRepo = getTestContext().get(key);
        return withAmbariRepoDetails(ambariRepo);
    }

    public AmbariTestDto withAmbariRepoDetails(AmbariRepositoryV4TestDto ambariRepoDetailsJson) {
        getRequest().setRepository(ambariRepoDetailsJson.getRequest());
        return this;
    }

    public AmbariTestDto withConfigStrategy(ConfigStrategy configStrategy) {
        getRequest().setConfigStrategy(configStrategy);
        return this;
    }

    public AmbariTestDto withAmbariSecurityMasterKey(String ambariSecurityMasterKey) {
        getRequest().setSecurityMasterKey(ambariSecurityMasterKey);
        return this;
    }
}
