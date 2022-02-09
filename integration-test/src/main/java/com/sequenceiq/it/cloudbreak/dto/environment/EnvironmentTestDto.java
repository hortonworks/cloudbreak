package com.sequenceiq.it.cloudbreak.dto.environment;

import static com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus.ARCHIVED;
import static com.sequenceiq.it.cloudbreak.config.azure.AzureMarketplaceImageProperties.AZURE_MARKETPLACE_FREEIPA_CATALOG_URL;
import static com.sequenceiq.it.cloudbreak.config.azure.AzureMarketplaceImageProperties.AZURE_MARKETPLACE_FREEIPA_IMAGE_UUID;
import static com.sequenceiq.it.cloudbreak.context.RunningParameter.emptyRunningParameter;
import static com.sequenceiq.it.cloudbreak.context.RunningParameter.key;
import static java.util.Objects.isNull;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.ws.rs.NotFoundException;

import org.testng.util.Strings;

import com.sequenceiq.common.api.backup.request.BackupRequest;
import com.sequenceiq.common.api.telemetry.request.TelemetryRequest;
import com.sequenceiq.common.api.type.Tunnel;
import com.sequenceiq.environment.api.v1.environment.endpoint.EnvironmentEndpoint;
import com.sequenceiq.environment.api.v1.environment.model.base.CloudStorageValidation;
import com.sequenceiq.environment.api.v1.environment.model.base.IdBrokerMappingSource;
import com.sequenceiq.environment.api.v1.environment.model.request.AttachedFreeIpaRequest;
import com.sequenceiq.environment.api.v1.environment.model.request.EnvironmentChangeCredentialRequest;
import com.sequenceiq.environment.api.v1.environment.model.request.EnvironmentNetworkRequest;
import com.sequenceiq.environment.api.v1.environment.model.request.EnvironmentRequest;
import com.sequenceiq.environment.api.v1.environment.model.request.FreeIpaImageRequest;
import com.sequenceiq.environment.api.v1.environment.model.request.LocationRequest;
import com.sequenceiq.environment.api.v1.environment.model.request.SecurityAccessRequest;
import com.sequenceiq.environment.api.v1.environment.model.request.aws.AwsEnvironmentParameters;
import com.sequenceiq.environment.api.v1.environment.model.request.azure.AzureEnvironmentParameters;
import com.sequenceiq.environment.api.v1.environment.model.request.gcp.GcpEnvironmentParameters;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus;
import com.sequenceiq.environment.api.v1.environment.model.response.SimpleEnvironmentResponse;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.api.model.FlowType;
import com.sequenceiq.it.cloudbreak.EnvironmentClient;
import com.sequenceiq.it.cloudbreak.Prototype;
import com.sequenceiq.it.cloudbreak.ResourceGroupTest;
import com.sequenceiq.it.cloudbreak.assign.Assignable;
import com.sequenceiq.it.cloudbreak.client.EnvironmentTestClient;
import com.sequenceiq.it.cloudbreak.context.Clue;
import com.sequenceiq.it.cloudbreak.context.Investigable;
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
        implements Searchable, Assignable, Investigable {

    public static final String ENVIRONMENT = "ENVIRONMENT";

    private static final String ENVIRONMENT_RESOURCE_NAME = "environmentName";

    private static final int ORDER = 100;

    @Inject
    private EnvironmentTestClient environmentTestClient;

    private Collection<SimpleEnvironmentResponse> simpleResponses;

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

    public String getParentEnvironmentName() {
        return getRequest().getParentEnvironmentName();
    }

    public AzureEnvironmentParameters getAzure() {
        return getRequest().getAzure();
    }

    public void setAzure(AzureEnvironmentParameters azure) {
        getRequest().setAzure(azure);
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
                .withNetwork()
                .withCloudStorageValidation(CloudStorageValidation.ENABLED);
    }

    public EnvironmentTestDto withCreateFreeIpa(Boolean create) {
        getRequest().getFreeIpa().setCreate(create);
        return this;
    }

    public EnvironmentTestDto withFreeIpaImage(String imageCatalog, String imageUuid) {
        if (!Strings.isNullOrEmpty(imageCatalog) && !Strings.isNullOrEmpty(imageUuid)) {
            FreeIpaImageRequest imageRequest = new FreeIpaImageRequest();
            imageRequest.setCatalog(imageCatalog);
            imageRequest.setId(imageUuid);
            withCreateFreeIpa(true);
            getRequest().getFreeIpa().setImage(imageRequest);
        }
        return this;
    }

    public EnvironmentTestDto withMarketplaceFreeIpaImage() {
        FreeIpaImageRequest imageRequest = new FreeIpaImageRequest();
        imageRequest.setCatalog(getMarketplaceFreeIpaCatalogUrl());
        imageRequest.setId(getMarketplaceFreeIpaImageUuid());
        getRequest().getFreeIpa().setImage(imageRequest);
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

    public EnvironmentTestDto withBackup(String backupLocation) {
        BackupRequest backupRequest = new BackupRequest();
        backupRequest.setStorageLocation(backupLocation);
        getRequest().setBackup(backupRequest);
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

    public EnvironmentTestDto withTunnel(Tunnel tunnel) {
        getRequest().setTunnel(tunnel);
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

    public EnvironmentTestDto withSecurityAccess() {
        EnvironmentSecurityAccessTestDto securityAccessTestDto = getTestContext().get(EnvironmentSecurityAccessTestDto.class);
        getRequest().setSecurityAccess(securityAccessTestDto.getRequest());
        return this;
    }

    public EnvironmentTestDto withSecurityAccess(SecurityAccessRequest securityAccessRequest) {
        getRequest().setSecurityAccess(securityAccessRequest);
        return this;
    }

    public EnvironmentTestDto withDefaultSecurityGroup(String securityGroup) {
        if (getRequest().getSecurityAccess() == null) {
            getRequest().setSecurityAccess(new SecurityAccessRequest());
        }
        getRequest().getSecurityAccess().setDefaultSecurityGroupId(securityGroup);
        getRequest().getSecurityAccess().setSecurityGroupIdForKnox(securityGroup);
        return this;
    }

    public EnvironmentTestDto withoutNetwork() {
        getRequest().setNetwork(null);
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

    public EnvironmentTestDto withResourceGroup(String resourceGroupUsage, String resourceGroupName) {
        return getCloudProvider().withResourceGroup(this, resourceGroupUsage, resourceGroupName);
    }

    public EnvironmentTestDto withResourceEncryption() {
        return getCloudProvider().withResourceEncryption(this);
    }

    public EnvironmentTestDto withAws(AwsEnvironmentParameters awsEnvironmentParameters) {
        getRequest().setAws(awsEnvironmentParameters);
        return this;
    }

    public EnvironmentTestDto withGcp(GcpEnvironmentParameters gcpEnvironmentParameters) {
        getRequest().setGcp(gcpEnvironmentParameters);
        return this;
    }

    public EnvironmentTestDto withParentEnvironment(RunningParameter runningParameter) {
        CloudbreakTestDto parentEnvDto = getTestContext().get(runningParameter.getKey());
        return withParentEnvironment(parentEnvDto);
    }

    public EnvironmentTestDto withParentEnvironment() {
        EnvironmentTestDto parentEnvDto = getTestContext().given(EnvironmentTestDto.class);
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
        return simpleResponses;
    }

    public void setResponseSimpleEnvSet(Collection<SimpleEnvironmentResponse> simpleResponses) {
        this.simpleResponses = simpleResponses;
    }

    public void setResponseSimpleEnv(SimpleEnvironmentResponse simpleResponse) {
        this.simpleResponse = simpleResponse;
    }

    @Override
    public List<SimpleEnvironmentResponse> getAll(EnvironmentClient client) {
        EnvironmentEndpoint environmentEndpoint = client.getDefaultClient().environmentV1Endpoint();
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
    public EnvironmentTestDto refresh() {
        LOGGER.info("Refresh Environment with name: {}", getName());
        return when(environmentTestClient.refresh(), key("refresh-environment-" + getName()));
    }

    @Override
    public void deleteForCleanup() {
        try {
            EnvironmentClient client = getClientForCleanup();
            client.getDefaultClient().environmentV1Endpoint().deleteByCrn(getCrn(), true, false);
            getTestContext().awaitWithClient(this, Map.of("status", ARCHIVED), client);
        } catch (NotFoundException nfe) {
            LOGGER.info("resource not found, thus cleanup not needed.");
        }
    }

    @Override
    public void delete(TestContext testContext, SimpleEnvironmentResponse entity, EnvironmentClient client) {
        LOGGER.info("Delete resource with name: {}", entity.getName());
        EnvironmentEndpoint credentialEndpoint = client.getDefaultClient().environmentV1Endpoint();
        credentialEndpoint.deleteByName(entity.getName(), true, false);
        setName(entity.getName());
        testContext.await(this, Map.of("status", ARCHIVED));
    }

    public EnvironmentTestDto await(EnvironmentStatus status) {
        return await(status, emptyRunningParameter());
    }

    public EnvironmentTestDto await(EnvironmentTestDto entity, EnvironmentStatus status, RunningParameter runningParameter) {
        return getTestContext().await(entity, Map.of("status", status), runningParameter);
    }

    public EnvironmentTestDto await(EnvironmentStatus status, RunningParameter runningParameter) {
        return getTestContext().await(this, Map.of("status", status), runningParameter);
    }

    public EnvironmentTestDto awaitForFlow() {
        return awaitForFlow(emptyRunningParameter());
    }

    @Override
    public EnvironmentTestDto awaitForFlow(RunningParameter runningParameter) {
        return getTestContext().awaitForFlow(this, runningParameter);
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

    private String getMarketplaceFreeIpaCatalogUrl() {
        return getTestParameter().get(AZURE_MARKETPLACE_FREEIPA_CATALOG_URL);
    }

    private String getMarketplaceFreeIpaImageUuid() {
        return getTestParameter().get(AZURE_MARKETPLACE_FREEIPA_IMAGE_UUID);
    }

    @Override
    public String getCrn() {
        if (getResponse() == null) {
            throw new IllegalStateException("Environment response hasn't been set, therefore 'getCrn' cannot be fulfilled.");
        }
        return getResponse().getCrn();
    }

    public String getResourceCrn() {
        if (getResponse() == null) {
            throw new IllegalStateException("Environment response hasn't been set, therefore 'getResourceCrn' cannot be fulfilled.");
        }
        return getResponse().getCrn();
    }

    @Override
    public Clue investigate() {
        if (getResponse() == null) {
            return null;
        }
        return new Clue("Environment", null, getResponse(), false);
    }

    public void setLastKnownFlow(FlowIdentifier flowIdentifier) {
        if (flowIdentifier.getType() == FlowType.FLOW) {
            setLastKnownFlowId(flowIdentifier.getPollableId());
        } else if (flowIdentifier.getType() == FlowType.FLOW_CHAIN) {
            setLastKnownFlowChainId(flowIdentifier.getPollableId());
        }
    }

    public EnvironmentTestDto waitingFor(Duration duration, String interruptedMessage) {
        getTestContext().waitingFor(duration, interruptedMessage);
        return this;
    }
}
