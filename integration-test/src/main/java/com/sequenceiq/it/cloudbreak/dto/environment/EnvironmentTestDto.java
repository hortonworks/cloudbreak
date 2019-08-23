package com.sequenceiq.it.cloudbreak.dto.environment;

import static com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus.ARCHIVED;
import static com.sequenceiq.it.cloudbreak.context.RunningParameter.emptyRunningParameter;
import static com.sequenceiq.it.cloudbreak.context.RunningParameter.force;
import static com.sequenceiq.it.cloudbreak.context.RunningParameter.key;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import com.sequenceiq.environment.api.v1.environment.endpoint.EnvironmentEndpoint;
import com.sequenceiq.environment.api.v1.environment.model.request.AttachedFreeIpaRequest;
import com.sequenceiq.environment.api.v1.environment.model.request.EnvironmentAuthenticationRequest;
import com.sequenceiq.environment.api.v1.environment.model.request.EnvironmentChangeCredentialRequest;
import com.sequenceiq.environment.api.v1.environment.model.request.EnvironmentNetworkRequest;
import com.sequenceiq.environment.api.v1.environment.model.request.EnvironmentRequest;
import com.sequenceiq.environment.api.v1.environment.model.request.LocationRequest;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus;
import com.sequenceiq.environment.api.v1.environment.model.response.SimpleEnvironmentResponse;
import com.sequenceiq.it.cloudbreak.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.EnvironmentClient;
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

    private static final String DUMMY_SSH_KEY = "ssh-rsa "
            + "AAAAB3NzaC1yc2EAAAADAQABAAABAQC0Rfl2G2vDs6yc19RxCqReunFgpYj+ucyLobpTCBtfDwzIbJot2Fmife6M42mBtiTmAK6x8kc"
            + "UEeab6CB4MUzsqF7vGTFUjwWirG/XU5pYXFUBhi8xzey+KS9KVrQ+UuKJh/AN9iSQeMV+rgT1yF5+etVH+bK1/37QCKp3+mCqjFzPyQOrvkGZv4sYyRwX7BKBLleQmIVWpofpj"
            + "T7BfcCxH877RzC5YMIi65aBc82Dl6tH6OEiP7mzByU52yvH6JFuwZ/9fWj1vXCWJzxx2w0F1OU8Zwg8gNNzL+SVb9+xfBE7xBHMpYFg72hBWPh862Ce36F4NZd3MpWMSjMmpDPh"
            + " centos";

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
                .environment(withName(resourceProperyProvider().getEnvironmentName())
                        .withDescription(resourceProperyProvider().getDescription("environment")))
                        .withCredentialName(getTestContext().get(CredentialTestDto.class).getName())
                        .withAuthentication(DUMMY_SSH_KEY);
    }

    public EnvironmentTestDto withCreateFreeIpa(Boolean create) {
        AttachedFreeIpaRequest attachedFreeIpaRequest = new AttachedFreeIpaRequest();
        attachedFreeIpaRequest.setCreate(create);
        getRequest().setFreeIpa(attachedFreeIpaRequest);
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

    public EnvironmentTestDto withAuthentication(String sshKey) {
        EnvironmentAuthenticationRequest authentication = new EnvironmentAuthenticationRequest();
        authentication.setPublicKey(sshKey);
        getRequest().setAuthentication(authentication);
        return this;
    }

    public EnvironmentTestDto withNetwork(EnvironmentNetworkRequest network) {
        getRequest().setNetwork(network);
        return this;
    }

    public EnvironmentTestDto withNetwork() {
        EnvironmentNetworkTestDto environmentNetwork = getCloudProvider().network(given(EnvironmentNetworkTestDto.class));
        if (environmentNetwork == null) {
            throw new IllegalArgumentException("Environment Network does not exist!");
        }
        return withNetwork(environmentNetwork.getRequest());
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
    public List<SimpleEnvironmentResponse> getAll(EnvironmentClient client) {
        EnvironmentEndpoint environmentEndpoint = client.getEnvironmentClient().environmentV1Endpoint();
        return new ArrayList<>(environmentEndpoint.list().getResponses()).stream()
                .filter(s -> s.getName() != null)
                .map(s -> {
                    SimpleEnvironmentResponse simpleEnvironmentResponse = new SimpleEnvironmentResponse();
                    simpleEnvironmentResponse.setName(s.getName());
                    return simpleEnvironmentResponse;
                }).collect(Collectors.toList());
    }

    @Override
    protected String name(SimpleEnvironmentResponse entity) {
        return entity.getName();
    }

    @Override
    public EnvironmentTestDto refresh(TestContext context, CloudbreakClient client) {
        LOGGER.info("Refresh resource with name: {}", getName());
        return when(environmentTestClient.describe(), key("refresh-environment-" + getName()));
    }

    @Override
    public void cleanUp(TestContext context, CloudbreakClient client) {
        LOGGER.info("Cleaning up resource with name: {}", getName());
        when(environmentTestClient.delete(), key("delete-environment-" + getName()).withSkipOnFail(false));
        await(ARCHIVED, force());
    }

    @Override
    public void delete(TestContext testContext, SimpleEnvironmentResponse entity, EnvironmentClient client) {
        LOGGER.info("Delete resource with name: {}", entity.getName());
        EnvironmentEndpoint credentialEndpoint = client.getEnvironmentClient().environmentV1Endpoint();
        credentialEndpoint.deleteByName(entity.getName());
        setName(entity.getName());
        testContext.await(this, ARCHIVED, emptyRunningParameter());
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
