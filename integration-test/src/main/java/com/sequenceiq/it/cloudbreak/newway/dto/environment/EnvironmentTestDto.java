package com.sequenceiq.it.cloudbreak.newway.dto.environment;

import static com.sequenceiq.it.cloudbreak.newway.util.ResponseUtil.getErrorMessage;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.ws.rs.WebApplicationException;

import com.sequenceiq.cloudbreak.api.endpoint.v4.environment.EnvironmentV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.environment.requests.EnvironmentV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.environment.requests.LocationV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.environment.responses.DetailedEnvironmentV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.environment.responses.SimpleEnvironmentV4Response;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.Prototype;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.dto.DeletableTestDto;
import com.sequenceiq.it.cloudbreak.newway.dto.credential.CredentialTestDto;

@Prototype
public class EnvironmentTestDto extends DeletableTestDto<EnvironmentV4Request, DetailedEnvironmentV4Response, EnvironmentTestDto, SimpleEnvironmentV4Response> {

    public static final String ENVIRONMENT = "ENVIRONMENT";

    private Collection<SimpleEnvironmentV4Response> response;

    private SimpleEnvironmentV4Response simpleResponse;

    public EnvironmentTestDto(TestContext testContext) {
        super(new EnvironmentV4Request(), testContext);
    }

    public EnvironmentTestDto() {
        super(ENVIRONMENT);
    }

    public EnvironmentTestDto(EnvironmentV4Request environmentV4Request, TestContext testContext) {
        super(environmentV4Request, testContext);
    }

    @Override
    public String getName() {
        return getRequest().getName();
    }

    @Override
    public EnvironmentTestDto valid() {
        return getCloudProvider().environment(withName(resourceProperyProvider().getName())
                .withDescription(resourceProperyProvider().getDescription("environment"))
                .withCredentialName(getTestContext().get(CredentialTestDto.class).getName()));
    }

    public EnvironmentTestDto withName(String name) {
        getRequest().setName(name);
        setName(name);
        return this;
    }

    public EnvironmentTestDto withCredential(String key) {
        CredentialTestDto credentialTestDto = getTestContext().get(key);
        getRequest().setCredential(credentialTestDto.getRequest());
        return this;
    }

    public EnvironmentTestDto withCredentialName(String name) {
        getRequest().setCredentialName(name);
        return this;
    }

    public EnvironmentTestDto withDescription(String description) {
        getRequest().setDescription(description);
        return this;
    }

    public EnvironmentTestDto withLdapConfigs(Set<String> ldap) {
        getRequest().setLdaps(ldap);
        return this;
    }

    public EnvironmentTestDto withKerberosConfigs(Set<String> kerberos) {
        getRequest().setKerberoses(kerberos);
        return this;
    }

    public EnvironmentTestDto withProxyConfigs(Set<String> proxy) {
        getRequest().setProxies(proxy);
        return this;
    }

    public EnvironmentTestDto withRegions(Set<String> region) {
        getRequest().setRegions(region);
        return this;
    }

    public EnvironmentTestDto withLocation(String location) {
        LocationV4Request locationV4Request = new LocationV4Request();
        locationV4Request.setName(location);
        getRequest().setLocation(locationV4Request);
        return this;
    }

    public EnvironmentTestDto withRdsConfigs(Set<String> rds) {
        getRequest().setDatabases(rds);
        return this;
    }

    @Override
    public void cleanUp(TestContext context, CloudbreakClient cloudbreakClient) {
        LOGGER.info("Cleaning up resource with name: {}", getName());
        try {
            SimpleEnvironmentV4Response entity = new SimpleEnvironmentV4Response();
            entity.setName(getName());
            delete(context, entity, cloudbreakClient);
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
    protected String name(SimpleEnvironmentV4Response entity) {
        return entity.getName();
    }

    @Override
    public void delete(TestContext testContext, SimpleEnvironmentV4Response entity, CloudbreakClient client) {
        try {
            client.getCloudbreakClient().environmentV4Endpoint().delete(client.getWorkspaceId(), entity.getName());
        } catch (Exception e) {
            LOGGER.warn("Something went wrong on {} ({}) purge. {}", entity.getName(), entity.getClass().getSimpleName(), getErrorMessage(e), e);
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