package com.sequenceiq.it.cloudbreak.dto.environment;

import java.util.Set;

import javax.inject.Inject;

import com.sequenceiq.environment.api.v1.environment.model.request.EnvironmentRequest;
import com.sequenceiq.environment.api.v1.environment.model.request.LocationRequest;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.SimpleEnvironmentResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.SimpleEnvironmentResponses;
import com.sequenceiq.it.cloudbreak.Prototype;
import com.sequenceiq.it.cloudbreak.client.CredentialTestClient;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.AbstractEnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.credential.CredentialTestDto;

@Prototype
public class EnvironmentTestDto extends AbstractEnvironmentTestDto<EnvironmentRequest, DetailedEnvironmentResponse, EnvironmentTestDto>  {

    public static final String ENVIRONMENT = "ENVIRONMENT";

    @Inject
    private CredentialTestClient credentialTestClient;

    private SimpleEnvironmentResponses responses;

    private SimpleEnvironmentResponse simpleResponse;

    public EnvironmentTestDto(TestContext testContext) {
        super(new EnvironmentRequest(), testContext);
    }

    public EnvironmentTestDto() {
        super(ENVIRONMENT);
    }

    public EnvironmentTestDto(EnvironmentRequest environmentRequest, TestContext testContext) {
        super(environmentRequest, testContext);
    }

    @Override
    public String getName() {
        return getRequest().getName();
    }

    @Override
    public EnvironmentTestDto valid() {
        return getCloudProvider()
                .environment(withName(resourceProperyProvider().getName())
                        .withDescription(resourceProperyProvider().getDescription("environment")))
                .withCredentialName(getTestContext().get(CredentialTestDto.class).getName());
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
        LocationRequest locationRequest = new LocationRequest();
        locationRequest.setName(location);
        getRequest().setLocation(locationRequest);
        return this;
    }

    public EnvironmentTestDto withCredentialName(String credentialName) {
        getRequest().setCredentialName(credentialName);
        return this;
    }

/*    @Override
    public void cleanUp(TestContext context, CloudbreakClient cloudbreakClient) {
        LOGGER.info("Cleaning up resource with name: {}", getName());
        try {
            SimpleEnvironmentResponse entity = new SimpleEnvironmentResponse();
            entity.setName(getName());
            delete(context, entity, cloudbreakClient);
        } catch (WebApplicationException ignore) {
            LOGGER.info("Something happend.");
        }
    }*/

    public SimpleEnvironmentResponses getResponseSimpleEnvSet() {
        return responses;
    }

    public void setResponseSimpleEnvSet(SimpleEnvironmentResponses responses) {
        this.responses = responses;
    }

    public SimpleEnvironmentResponse getResponseSimpleEnv() {
        return simpleResponse;
    }

    public void setResponseSimpleEnv(SimpleEnvironmentResponse simpleResponse) {
        this.simpleResponse = simpleResponse;
    }

    @Override
    public int order() {
        return 600;
    }
}
