package com.sequenceiq.it.cloudbreak.newway.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.ws.rs.WebApplicationException;

import com.sequenceiq.cloudbreak.api.endpoint.v4.environment.EnvironmentV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.environment.responses.DetailedEnvironmentV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.environment.responses.SimpleEnvironmentV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.environment.EnvironmentSettingsV4Request;
import com.sequenceiq.it.cloudbreak.newway.AbstractCloudbreakEntity;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.CredentialEntity;
import com.sequenceiq.it.cloudbreak.newway.Prototype;
import com.sequenceiq.it.cloudbreak.newway.context.Purgable;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;

@Prototype
public class EnvironmentSettingsV4Entity extends AbstractCloudbreakEntity<EnvironmentSettingsV4Request, DetailedEnvironmentV4Response, EnvironmentSettingsV4Entity>
        implements Purgable<SimpleEnvironmentV4Response> {

    public static final String ENVIRONMENT = "ENVIRONMENT";

    private static final String VALID_REGION = "Europe";

    private static final String VALID_LOCATION = "London";

    private static final String AVAILABILITY_ZONE = "London";

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
        PlacementSettingsEntity placementSettings = getTestContext().get(PlacementSettingsEntity.class)
                .withRegion(VALID_REGION)
                .withAvailabilityZone(AVAILABILITY_ZONE);

        return withName(getNameCreator().getRandomNameForMock())
                .withPlacement(placementSettings)
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

    public EnvironmentSettingsV4Entity withPlacement(String key) {
        PlacementSettingsEntity placementSettings = getTestContext().get(key);
        return withPlacement(placementSettings);
    }

    public EnvironmentSettingsV4Entity withPlacement(PlacementSettingsEntity placementSettings) {
        getRequest().setPlacement(placementSettings.getRequest());
        return this;
    }


    @Override
    public void cleanUp(TestContext context, CloudbreakClient cloudbreakClient) {
        LOGGER.info("Cleaning up resource with name: {}", getName());
        try {
            SimpleEnvironmentV4Response entity = new SimpleEnvironmentV4Response();
            entity.setName(getName());
            delete(entity, cloudbreakClient);
        } catch (WebApplicationException ignore) {
            LOGGER.info("Something happend.");
        }
    }

    public Set<SimpleEnvironmentV4Response> getResponseSimpleEnvSet() {
        return response;
    }

    public void setResponseSimpleEnvSet(Set<SimpleEnvironmentV4Response> response) {
        this.response = response;
    }

    public SimpleEnvironmentV4Response getResponseSimpleEnv() {
        return simpleResponse;
    }

    public void setResponseSimpleEnv(SimpleEnvironmentV4Response simpleResponse) {
        this.simpleResponse = simpleResponse;
    }

    @Override
    public List<SimpleEnvironmentV4Response> getAll(CloudbreakClient client) {
        EnvironmentV4Endpoint environmentV4Endpoint = client.getCloudbreakClient().environmentV3Endpoint();
        return new ArrayList<>(environmentV4Endpoint.list(client.getWorkspaceId()).getResponses());
    }

    @Override
    public boolean deletable(SimpleEnvironmentV4Response entity) {
        return entity.getName().startsWith("mock-");
    }

    @Override
    public void delete(SimpleEnvironmentV4Response entity, CloudbreakClient client) {
        try {
            client.getCloudbreakClient().environmentV3Endpoint().delete(client.getWorkspaceId(), entity.getName());
        } catch (Exception e) {
            LOGGER.warn("Something went wrong on {} ({}) purge. {}", entity.getName(), entity.getClass().getSimpleName(), e.getMessage(), e);
        }
    }

    @Override
    public int order() {
        return 500;
    }
}