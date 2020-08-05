package com.sequenceiq.it.cloudbreak.dto.environment;

import static com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus.ARCHIVED;
import static com.sequenceiq.it.cloudbreak.context.RunningParameter.emptyRunningParameter;
import static com.sequenceiq.it.cloudbreak.context.RunningParameter.key;
import static java.util.Objects.isNull;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Inject;

import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.common.api.telemetry.request.TelemetryRequest;
import com.sequenceiq.environment.api.v1.environment.endpoint.EnvironmentEndpoint;
import com.sequenceiq.environment.api.v1.environment.model.base.CloudStorageValidation;
import com.sequenceiq.environment.api.v1.environment.model.base.IdBrokerMappingSource;
import com.sequenceiq.environment.api.v1.environment.model.request.AttachedFreeIpaRequest;
import com.sequenceiq.environment.api.v1.environment.model.request.EnvironmentChangeCredentialRequest;
import com.sequenceiq.environment.api.v1.environment.model.request.EnvironmentNetworkRequest;
import com.sequenceiq.environment.api.v1.environment.model.request.EnvironmentRequest;
import com.sequenceiq.environment.api.v1.environment.model.request.LocationRequest;
import com.sequenceiq.environment.api.v1.environment.model.request.aws.AwsEnvironmentParameters;
import com.sequenceiq.environment.api.v1.environment.model.request.azure.AzureEnvironmentParameters;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus;
import com.sequenceiq.environment.api.v1.environment.model.response.SimpleEnvironmentResponse;
import com.sequenceiq.it.cloudbreak.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.EnvironmentClient;
import com.sequenceiq.it.cloudbreak.Prototype;
import com.sequenceiq.it.cloudbreak.ResourceGroupTest;
import com.sequenceiq.it.cloudbreak.assign.Assignable;
import com.sequenceiq.it.cloudbreak.client.EnvironmentTestClient;
import com.sequenceiq.it.cloudbreak.cloud.v4.aws.AwsProperties;
import com.sequenceiq.it.cloudbreak.context.RunningParameter;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.CloudbreakTestDto;
import com.sequenceiq.it.cloudbreak.dto.DeletableEnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.credential.CredentialTestDto;
import com.sequenceiq.it.cloudbreak.dto.telemetry.TelemetryTestDto;
import com.sequenceiq.it.cloudbreak.search.Searchable;

@Prototype
public class EnvironmentTestDto
        extends DeletableEnvironmentTestDto<EnvironmentRequest, DetailedEnvironmentResponse, EnvironmentTestDto, SimpleEnvironmentResponse>
        implements Searchable, Assignable {

    public static final String ENVIRONMENT = "ENVIRONMENT";

    private static final String ENVIRONMENT_RESOURCE_NAME = "environmentName";

    private static final int ORDER = 600;

    @Inject
    private EnvironmentTestClient environmentTestClient;

    @Inject
    private AwsProperties awsProperties;

    private Collection<SimpleEnvironmentResponse> response;

    private SimpleEnvironmentResponse simpleResponse;

    private EnvironmentChangeCredentialRequest enviornmentChangeCredentialRequest;

    private int order = ORDER;

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

    public String getParentEnvironmentName() {
        return getRequest().getParentEnvironmentName();
    }

    @Override
    public EnvironmentTestDto valid() {
        return getCloudProvider()
                .environment(withName(getResourcePropertyProvider().getEnvironmentName(getCloudPlatform()))
                        .withDescription(getResourcePropertyProvider().getDescription("environment")))
                .withCredentialName(getTestContext().get(CredentialTestDto.class).getName())
                .withAuthentication(getTestContext().given(EnvironmentAuthenticationTestDto.class))
                .withCloudplatform(getCloudPlatform().toString())
                .withIdBrokerMappingSource(IdBrokerMappingSource.MOCK)
                .withResourceGroup(getResourceGroupUsage(), getResourceGroupName())
                .withCloudStorageValidation(CloudStorageValidation.ENABLED);
    }

    public EnvironmentTestDto withCreateFreeIpa(Boolean create) {
        getRequest().getFreeIpa().setCreate(create);
        return this;
    }

    public EnvironmentTestDto withFreeIpa(AttachedFreeIpaRequest attachedFreeIpaRequest) {
        getRequest().setFreeIpa(attachedFreeIpaRequest);
        return this;
    }

    public EnvironmentTestDto withMockIDBMS() {
        getRequest().setIdBrokerMappingSource(IdBrokerMappingSource.MOCK);
        return this;
    }

    public EnvironmentTestDto withName(String name) {
        getRequest().setName(name);
        setName(name);
        return this;
    }

    @Override
    public String getResourceNameType() {
        return ENVIRONMENT_RESOURCE_NAME;
    }

    public EnvironmentTestDto withIdBrokerMappingSource(IdBrokerMappingSource idBrokerMappingSource) {
        getRequest().setIdBrokerMappingSource(idBrokerMappingSource);
        return this;
    }

    public EnvironmentTestDto withCloudStorageValidation(CloudStorageValidation cloudStorageValidation) {
        getRequest().setCloudStorageValidation(cloudStorageValidation);
        return this;
    }

    public EnvironmentTestDto withTelemetry(String telemetry) {
        TelemetryTestDto telemetryTestDto = getTestContext().get(telemetry);
        getRequest().setTelemetry(telemetryTestDto.getRequest());
        return this;
    }

    public EnvironmentTestDto withDescription(String description) {
        getRequest().setDescription(description);
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

    public EnvironmentTestDto withAuthentication(EnvironmentAuthenticationTestDto authentication) {
        getRequest().setAuthentication(authentication.getRequest());
        return this;
    }

    public EnvironmentTestDto withAuthentication() {
        EnvironmentAuthenticationTestDto authenticationTestDto = getTestContext().get(EnvironmentAuthenticationTestDto.class);
        getRequest().setAuthentication(authenticationTestDto.getRequest());
        return this;
    }

    public EnvironmentTestDto withSecurityAccess() {
        EnvironmentSecurityAccessTestDto securityAccessTestDto = getTestContext().get(EnvironmentSecurityAccessTestDto.class);
        getRequest().setSecurityAccess(securityAccessTestDto.getRequest());
        return this;
    }

    public EnvironmentTestDto withNetwork(EnvironmentNetworkRequest network) {
        getRequest().setNetwork(network);
        return this;
    }

    public EnvironmentTestDto withTelemetry(TelemetryRequest telemetry) {
        getRequest().setTelemetry(telemetry);
        return this;
    }

    public EnvironmentTestDto withNetwork() {
        EnvironmentNetworkTestDto environmentNetwork = getCloudProvider().network(given(EnvironmentNetworkTestDto.class));
        if (environmentNetwork == null) {
            throw new IllegalArgumentException("Environment Network does not exist!");
        }
        return withNetwork(environmentNetwork.getRequest());
    }

    public EnvironmentTestDto withNetwork(String key) {
        EnvironmentNetworkTestDto environmentNetwork = getTestContext().get(key);
        if (environmentNetwork == null) {
            throw new IllegalArgumentException("Environment Network does not exist!");
        }
        return withNetwork(environmentNetwork.getRequest());
    }

    public EnvironmentTestDto withS3Guard(String tableName) {
        getCloudProvider().setS3Guard(this, tableName);
        return this;
    }

    public EnvironmentTestDto withResourceGroup(String resourceGroupUsage, String resourceGroupName) {
        return getCloudProvider().withResourceGroup(this, resourceGroupUsage, resourceGroupName);
    }

    public EnvironmentTestDto withAws(AwsEnvironmentParameters awsEnvironmentParameters) {
        getRequest().setAws(awsEnvironmentParameters);
        return this;
    }

    public EnvironmentTestDto withS3Guard() {
        if (CloudPlatform.AWS.equals(getTestContext().getCloudProvider().getCloudPlatform())) {
            String tableName = awsProperties.getDynamoTableName() + '-' + UUID.randomUUID().toString();
            return withS3Guard(tableName);
        } else {
            LOGGER.info("S3guard is ignored on cloudplatform {}.", getTestContext().getCloudProvider().getCloudPlatform());
        }
        return this;
    }

    public EnvironmentTestDto withAzure(AzureEnvironmentParameters azureEnvironmentParameters) {
        getRequest().setAzure(azureEnvironmentParameters);
        return this;
    }

    public EnvironmentTestDto withParentEnvironment(RunningParameter runningParameter) {
        CloudbreakTestDto parentEnvDto = getTestContext().get(runningParameter.getKey());
        return withParentEnvironment(parentEnvDto);
    }

    public EnvironmentTestDto withParentEnvironment() {
        CloudbreakTestDto parentEnvDto = getTestContext().given(EnvironmentTestDto.class);
        return withParentEnvironment(parentEnvDto);
    }

    public EnvironmentTestDto addTags(Map<String, String> tags) {
        getRequest().getTags().putAll(tags);
        return this;
    }

    private EnvironmentTestDto withParentEnvironment(CloudbreakTestDto parentEnvDto) {
        if (isNull(parentEnvDto)) {
            order = ORDER;
        } else {
            getRequest().setParentEnvironmentName(parentEnvDto.getName());
            order = ORDER - 1;
        }
        return withNetwork();
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
        if (getResponse() != null) {
            when(environmentTestClient.cascadingDelete(), key("delete-environment-" + getName()).withSkipOnFail(false));
            await(ARCHIVED, new RunningParameter().withSkipOnFail(true));
        } else {
            LOGGER.info("Response field is null for env: {}", getName());
        }
    }

    @Override
    public void delete(TestContext testContext, SimpleEnvironmentResponse entity, EnvironmentClient client) {
        LOGGER.info("Delete resource with name: {}", entity.getName());
        EnvironmentEndpoint credentialEndpoint = client.getEnvironmentClient().environmentV1Endpoint();
        credentialEndpoint.deleteByName(entity.getName(), true, false);
        setName(entity.getName());
        await(this, ARCHIVED, emptyRunningParameter());
    }

    public EnvironmentTestDto await(EnvironmentStatus status) {
        return await(status, emptyRunningParameter());
    }

    public EnvironmentTestDto await(EnvironmentTestDto entity, EnvironmentStatus status, RunningParameter runningParameter) {
        return getTestContext().await(entity, status, runningParameter);
    }

    public EnvironmentTestDto await(EnvironmentStatus status, RunningParameter runningParameter) {
        return getTestContext().await(this, status, runningParameter);
    }

    public EnvironmentTestDto await(EnvironmentStatus status, RunningParameter runningParameter, Duration pollingInterval) {
        return getTestContext().await(this, status, runningParameter, pollingInterval);
    }

    public EnvironmentTestDto await(EnvironmentStatus status, Duration pollingInterval) {
        return await(status, emptyRunningParameter(), pollingInterval);
    }

    @Override
    public int order() {
        return order;
    }

    public EnvironmentChangeCredentialRequest getEnviornmentChangeCredentialRequest() {
        return enviornmentChangeCredentialRequest;
    }

    public EnvironmentTestDto withChangeCredentialName(String name) {
        enviornmentChangeCredentialRequest = new EnvironmentChangeCredentialRequest();
        enviornmentChangeCredentialRequest.setCredentialName(name);
        return this;
    }

    @Override
    public String getSearchId() {
        return getName();
    }

    private EnvironmentTestDto withCloudplatform(String platform) {
        return this;
    }

    private String getResourceGroupName() {
        return getTestParameter().get(ResourceGroupTest.AZURE_RESOURCE_GROUP_NAME);
    }

    private String getResourceGroupUsage() {
        return getTestParameter().get(ResourceGroupTest.AZURE_RESOURCE_GROUP_USAGE);
    }

    @Override
    public String getCrn() {
        if (getResponse() == null) {
            throw new IllegalStateException("You have tried to assign to a Dto that hasn't been created and therefore has no Response object.");
        }
        return getResponse().getCrn();
    }
}
