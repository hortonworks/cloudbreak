package com.sequenceiq.it.cloudbreak.newway;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.ws.rs.WebApplicationException;

import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.requests.CredentialV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.environment.EnvironmentV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.environment.requests.EnvironmentV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.environment.requests.LocationV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.environment.responses.DetailedEnvironmentV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.environment.responses.SimpleEnvironmentV4Response;
import com.sequenceiq.it.cloudbreak.newway.context.Purgable;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;

@Prototype
public class EnvironmentEntity extends AbstractCloudbreakEntity<EnvironmentV4Request, DetailedEnvironmentV4Response, EnvironmentEntity>
        implements Purgable<SimpleEnvironmentV4Response> {

    public static final String ENVIRONMENT = "ENVIRONMENT";

    public static final String EUROPE = "Europe";

    public static final Set<String> VALID_REGION = Collections.singleton(EUROPE);

    public static final String AVAILABILITY_ZONE = "London";

    public static final String LONDON = "London";

    private Collection<SimpleEnvironmentV4Response> response;

    private SimpleEnvironmentV4Response simpleResponse;

    public EnvironmentEntity(TestContext testContext) {
        super(new EnvironmentV4Request(), testContext);
    }

    public EnvironmentEntity() {
        super(ENVIRONMENT);
    }

    public EnvironmentEntity(EnvironmentV4Request environmentV4Request, TestContext testContext) {
        super(environmentV4Request, testContext);
    }

    @Override
    public String getName() {
        return getRequest().getName();
    }

    @Override
    public EnvironmentEntity valid() {
        return withName(getNameCreator().getRandomNameForMock())
                .withDescription("Description for environment")
                .withRegions(VALID_REGION)
                .withLocation(LONDON)
                .withCredentialName(getTestContext().get(CredentialEntity.class).getName());
    }

    public EnvironmentEntity withName(String name) {
        getRequest().setName(name);
        setName(name);
        return this;
    }

    public EnvironmentEntity withCredential(CredentialV4Request credential) {
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
        getRequest().setLdaps(ldap);
        return this;
    }

    public EnvironmentEntity withProxyConfigs(Set<String> proxy) {
        getRequest().setProxies(proxy);
        return this;
    }

    public EnvironmentEntity withRegions(Set<String> region) {
        getRequest().setRegions(region);
        return this;
    }

    public EnvironmentEntity withLocation(String location) {
        LocationV4Request locationV4Request = new LocationV4Request();
        locationV4Request.setLocationName(location);
        getRequest().setLocation(locationV4Request);
        return this;
    }

    public EnvironmentEntity withRdsConfigs(Set<String> rds) {
        getRequest().setDatabases(rds);
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

    public Collection<SimpleEnvironmentV4Response> getResponseSimpleEnvSet() {
        return response;
    }

    public void setResponseSimpleEnvSet(Collection<SimpleEnvironmentV4Response> response) {
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
        EnvironmentV4Endpoint environmentV4Endpoint = client.getCloudbreakClient().environmentV4Endpoint();
        return new ArrayList<>(environmentV4Endpoint.list(client.getWorkspaceId()).getResponses());
    }

    @Override
    public boolean deletable(SimpleEnvironmentV4Response entity) {
        return entity.getName().startsWith("mock-");
    }

    @Override
    public void delete(SimpleEnvironmentV4Response entity, CloudbreakClient client) {
        try {
            client.getCloudbreakClient().environmentV4Endpoint().delete(client.getWorkspaceId(), entity.getName());
        } catch (Exception e) {
            LOGGER.warn("Something went wrong on {} ({}) purge. {}", entity.getName(), entity.getClass().getSimpleName(), e.getMessage(), e);
        }
    }

    @Override
    public int order() {
        return 600;
    }

    public String getCredentialName() {
        return getRequest().getCredentialName();
    }
}