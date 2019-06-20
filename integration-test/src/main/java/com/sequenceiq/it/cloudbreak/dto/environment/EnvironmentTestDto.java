package com.sequenceiq.it.cloudbreak.dto.environment;

import static com.sequenceiq.it.cloudbreak.context.RunningParameter.emptyRunningParameter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;

import com.sequenceiq.environment.api.v1.environment.model.request.EnvironmentChangeCredentialRequest;
import com.sequenceiq.environment.api.v1.environment.model.request.EnvironmentRequest;
import com.sequenceiq.environment.api.v1.environment.model.request.LocationRequest;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus;
import com.sequenceiq.environment.api.v1.environment.model.response.SimpleEnvironmentResponse;
import com.sequenceiq.it.cloudbreak.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.Prototype;
import com.sequenceiq.it.cloudbreak.client.EnvironmentTestClient;
import com.sequenceiq.it.cloudbreak.context.RunningParameter;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.DeletableEnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.credential.CredentialTestDto;

@Prototype
public class EnvironmentTestDto
        extends DeletableEnvironmentTestDto<EnvironmentRequest, DetailedEnvironmentResponse, EnvironmentTestDto, SimpleEnvironmentResponse> {

    public static final String ENVIRONMENT = "ENVIRONMENT";

    @Inject
    private EnvironmentTestClient environmentTestClient;

    private Collection<SimpleEnvironmentResponse> response;

    private SimpleEnvironmentResponse simpleResponse;

    private EnvironmentChangeCredentialRequest enviornmentChangeCredentialRequest;

    public EnvironmentTestDto(TestContext testContext) {
        super(new EnvironmentRequest(), testContext);
    }

    public EnvironmentTestDto() {
        super(ENVIRONMENT);
    }

    public EnvironmentTestDto(EnvironmentRequest environmentV4Request, TestContext testContext) {
        super(environmentV4Request, testContext);
    }

    @Override
    public String getName() {
        return getRequest().getName();
    }

    @Override
    public EnvironmentTestDto valid() {
        return getCloudProvider()
                .environment(withName(resourceProperyProvider().getName())
                        .withCreateFreeIpa(Boolean.FALSE)
                        .withDescription(resourceProperyProvider().getDescription("environment")))
                        .withCredentialName(getTestContext().get(CredentialTestDto.class).getName());
    }

    private EnvironmentTestDto withCreateFreeIpa(Boolean create) {
        getRequest().setCreateFreeIpa(create);
        return this;
    }

    public EnvironmentTestDto withName(String name) {
        getRequest().setName(name);
        setName(name);
        return this;
    }

    public EnvironmentTestDto withDescription(String description) {
        getRequest().setDescription(description);
        return this;
    }

    public EnvironmentTestDto withRegions(Set<String> region) {
        getRequest().setRegions(region);
        return this;
    }

    public EnvironmentTestDto withLocation(String location) {
        LocationRequest locationV4Request = new LocationRequest();
        locationV4Request.setName(location);
        getRequest().setLocation(locationV4Request);
        return this;
    }

    public EnvironmentTestDto withCredentialName(String credentialName) {
        getRequest().setCredentialName(credentialName);
        return this;
    }

    @Override
    public void cleanUp(TestContext context, CloudbreakClient cloudbreakClient) {
        LOGGER.info("Cleaning up resource with name: {}", getName());
        try {
            SimpleEnvironmentResponse entity = new SimpleEnvironmentResponse();
            entity.setName(getName());
            delete(context, entity, cloudbreakClient);
        } catch (WebApplicationException ignore) {
            LOGGER.info("Something happend.");
        }
    }

    public Collection<SimpleEnvironmentResponse> getResponseSimpleEnvSet() {
        return response;
    }

    public void setResponseSimpleEnvSet(Collection<SimpleEnvironmentResponse> response) {
        this.response = response;
    }

    public SimpleEnvironmentResponse getResponseSimpleEnv() {
        return simpleResponse;
    }

    public void setResponseSimpleEnv(SimpleEnvironmentResponse simpleResponse) {
        this.simpleResponse = simpleResponse;
    }

    @Override
    public List<SimpleEnvironmentResponse> getAll(CloudbreakClient client) {
        return new ArrayList<>(when(environmentTestClient.list()).getResponseSimpleEnvSet());
    }

    @Override
    protected String name(SimpleEnvironmentResponse entity) {
        return entity.getName();
    }

    @Override
    public void delete(TestContext testContext, SimpleEnvironmentResponse entity, CloudbreakClient client) {
        when(environmentTestClient.delete());
    }

    public EnvironmentTestDto await(EnvironmentStatus status) {
        return await(status, emptyRunningParameter());
    }

    public EnvironmentTestDto await(EnvironmentStatus status, RunningParameter runningParameter) {
        return getTestContext().await(this, status, runningParameter);
    }

    @Override
    public int order() {
        return 600;
    }

    public EnvironmentChangeCredentialRequest getEnviornmentChangeCredentialRequest() {
        return enviornmentChangeCredentialRequest;
    }

    public void setEnviornmentChangeCredentialRequest(EnvironmentChangeCredentialRequest enviornmentChangeCredentialRequest) {
        this.enviornmentChangeCredentialRequest = enviornmentChangeCredentialRequest;
    }

    public EnvironmentTestDto withChangeCredentialName(String name) {
        enviornmentChangeCredentialRequest = new EnvironmentChangeCredentialRequest();
        enviornmentChangeCredentialRequest.setCredentialName(name);
        return this;
    }
}
