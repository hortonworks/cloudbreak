package com.sequenceiq.it.cloudbreak.newway;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.WebApplicationException;

import com.sequenceiq.cloudbreak.api.model.CredentialRequest;
import com.sequenceiq.cloudbreak.api.model.environment.request.EnvironmentRequest;
import com.sequenceiq.cloudbreak.api.model.environment.request.LocationRequest;
import com.sequenceiq.cloudbreak.api.model.environment.response.DetailedEnvironmentResponse;
import com.sequenceiq.cloudbreak.api.model.environment.response.SimpleEnvironmentResponse;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;

@Prototype
public class EnvironmentEntity extends AbstractCloudbreakEntity<EnvironmentRequest, DetailedEnvironmentResponse, EnvironmentEntity>  {

    public static final String ENVIRONMENT = "ENVIRONMENT";

    private static final Set<String> VALID_REGION = new HashSet<>(Collections.singletonList("Europe"));

    private static final String VALID_LOCATION = "London";

    private Set<SimpleEnvironmentResponse> response;

    private SimpleEnvironmentResponse simpleResponse;

    public EnvironmentEntity(TestContext testContext) {
        super(new EnvironmentRequest(), testContext);
    }

    public EnvironmentEntity() {
        super(ENVIRONMENT);
    }

    public EnvironmentEntity(EnvironmentRequest environmentRequest, TestContext testContext) {
        super(environmentRequest, testContext);
    }

    @Override
    public EnvironmentEntity valid() {
        return withName(getNameCreator().getRandomNameForMock())
                .withDescription("Description for environment")
                .withRegions(VALID_REGION)
                .withLocation(VALID_LOCATION)
                .withCredentialName(getTestContext().get(CredentialEntity.class).getName());
    }

    public EnvironmentEntity withName(String name) {
        getRequest().setName(name);
        setName(name);
        return this;
    }

    public EnvironmentEntity withCredential(CredentialRequest credential) {
        getRequest().setCredential(credential);
        return this;
    }

    public EnvironmentEntity withCredentialName(String name) {
        getRequest().setCredentialName(name);
        return this;
    }

    public EnvironmentEntity withDescription(String description) {
        getRequest().setDescription(description);
        return this;
    }

    public EnvironmentEntity withLdapConfigs(Set<String> ldap) {
        getRequest().setLdapConfigs(ldap);
        return this;
    }

    public EnvironmentEntity withProxyConfigs(Set<String> proxy) {
        getRequest().setProxyConfigs(proxy);
        return this;
    }

    public EnvironmentEntity withRegions(Set<String> region) {
        getRequest().setRegions(region);
        return this;
    }

    public EnvironmentEntity withLocation(String location) {
        LocationRequest locationRequest = new LocationRequest();
        locationRequest.setLocationName(location);
        getRequest().setLocation(locationRequest);
        return this;
    }

    public EnvironmentEntity withRdsConfigs(Set<String> rds) {
        getRequest().setRdsConfigs(rds);
        return this;
    }

    @Override
    public void cleanUp(TestContext context, CloudbreakClient cloudbreakClient) {
        LOGGER.info("Cleaning up resource with name: {}", getName());
        try {
            cloudbreakClient.getCloudbreakClient().environmentV3Endpoint().delete(cloudbreakClient.getWorkspaceId(), getName());
        } catch (WebApplicationException ignore) {
            LOGGER.info("Something happend.");
        }
    }

    public Set<SimpleEnvironmentResponse> getResponseSimpleEnvSet() {
        return response;
    }

    public void setResponseSimpleEnvSet(Set<SimpleEnvironmentResponse> response) {
        this.response = response;
    }

    public SimpleEnvironmentResponse getResponseSimpleEnv() {
        return simpleResponse;
    }

    public void setResponseSimpleEnv(SimpleEnvironmentResponse response) {
        this.simpleResponse = response;
    }
}