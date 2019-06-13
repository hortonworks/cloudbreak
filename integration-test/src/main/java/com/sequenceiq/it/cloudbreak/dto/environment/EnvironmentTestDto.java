package com.sequenceiq.it.cloudbreak.dto.environment;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.ws.rs.WebApplicationException;

import com.sequenceiq.cloudbreak.api.endpoint.v4.environment.EnvironmentV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.environment.responses.SimpleEnvironmentV4Response;
import com.sequenceiq.environment.api.v1.environment.model.request.EnvironmentRequest;
import com.sequenceiq.environment.api.v1.environment.model.request.LocationRequest;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.SimpleEnvironmentResponse;
import com.sequenceiq.it.cloudbreak.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.Prototype;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.DeletableTestDto;
import com.sequenceiq.it.cloudbreak.util.ResponseUtil;

@Prototype
public class EnvironmentTestDto extends DeletableTestDto<EnvironmentRequest, DetailedEnvironmentResponse, EnvironmentTestDto, SimpleEnvironmentResponse> {

    public static final String ENVIRONMENT = "ENVIRONMENT";

    private Collection<SimpleEnvironmentResponse> response;

    private SimpleEnvironmentV4Response simpleResponse;

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
                        .withDescription(resourceProperyProvider().getDescription("environment")));
//                .withCredential(getTestContext().get(CredentialTestDto.class).getName()));
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

    public SimpleEnvironmentV4Response getResponseSimpleEnv() {
        return simpleResponse;
    }

    public void setResponseSimpleEnv(SimpleEnvironmentV4Response simpleResponse) {
        this.simpleResponse = simpleResponse;
    }

    @Override
    public List<SimpleEnvironmentResponse> getAll(CloudbreakClient client) {
        EnvironmentV4Endpoint environmentV4Endpoint = client.getCloudbreakClient().environmentV4Endpoint();
        return new ArrayList<>(environmentV4Endpoint.list(client.getWorkspaceId()).getResponses());
    }

    @Override
    protected String name(SimpleEnvironmentResponse entity) {
        return entity.getName();
    }

    @Override
    public void delete(TestContext testContext, SimpleEnvironmentResponse entity, CloudbreakClient client) {
        try {
            client.getCloudbreakClient().environmentV4Endpoint().delete(client.getWorkspaceId(), entity.getName());
        } catch (Exception e) {
            LOGGER.warn("Something went wrong on {} ({}) purge. {}", entity.getName(), entity.getClass().getSimpleName(), ResponseUtil.getErrorMessage(e), e);
        }
    }

    @Override
    public int order() {
        return 600;
    }
}
