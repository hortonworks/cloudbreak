package com.sequenceiq.it.cloudbreak.dto.environment;

import static com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus.ARCHIVED;
import static com.sequenceiq.it.cloudbreak.context.RunningParameter.emptyRunningParameter;
import static com.sequenceiq.it.cloudbreak.context.RunningParameter.key;
import static java.util.Objects.isNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;

import org.testng.util.Strings;

import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredEvent;
import com.sequenceiq.cloudbreak.structuredevent.rest.endpoint.CDPStructuredEventV1Endpoint;
import com.sequenceiq.common.api.backup.request.BackupRequest;
import com.sequenceiq.common.api.telemetry.request.FeaturesRequest;
import com.sequenceiq.common.api.telemetry.request.TelemetryRequest;
import com.sequenceiq.common.api.type.EnvironmentType;
import com.sequenceiq.common.api.type.FeatureSetting;
import com.sequenceiq.common.api.type.Tunnel;
import com.sequenceiq.common.model.Architecture;
import com.sequenceiq.common.model.SeLinux;
import com.sequenceiq.environment.api.v1.environment.endpoint.EnvironmentEndpoint;
import com.sequenceiq.environment.api.v1.environment.model.CustomDockerRegistryV1Parameters;
import com.sequenceiq.environment.api.v1.environment.model.base.CloudStorageValidation;
import com.sequenceiq.environment.api.v1.environment.model.base.IdBrokerMappingSource;
import com.sequenceiq.environment.api.v1.environment.model.request.AttachedFreeIpaRequest;
import com.sequenceiq.environment.api.v1.environment.model.request.DataServicesRequest;
import com.sequenceiq.environment.api.v1.environment.model.request.EnvironmentChangeCredentialRequest;
import com.sequenceiq.environment.api.v1.environment.model.request.EnvironmentNetworkRequest;
import com.sequenceiq.environment.api.v1.environment.model.request.EnvironmentRequest;
import com.sequenceiq.environment.api.v1.environment.model.request.ExternalizedComputeCreateRequest;
import com.sequenceiq.environment.api.v1.environment.model.request.FreeIpaImageRequest;
import com.sequenceiq.environment.api.v1.environment.model.request.FreeIpaSecurityRequest;
import com.sequenceiq.environment.api.v1.environment.model.request.LocationRequest;
import com.sequenceiq.environment.api.v1.environment.model.request.SecurityAccessRequest;
import com.sequenceiq.environment.api.v1.environment.model.request.aws.AwsEnvironmentParameters;
import com.sequenceiq.environment.api.v1.environment.model.request.aws.AwsFreeIpaParameters;
import com.sequenceiq.environment.api.v1.environment.model.request.azure.AzureEnvironmentParameters;
import com.sequenceiq.environment.api.v1.environment.model.request.gcp.GcpEnvironmentParameters;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus;
import com.sequenceiq.environment.api.v1.environment.model.response.SimpleEnvironmentResponse;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.api.model.FlowType;
import com.sequenceiq.it.cloudbreak.Prototype;
import com.sequenceiq.it.cloudbreak.assign.Assignable;
import com.sequenceiq.it.cloudbreak.client.EnvironmentTestClient;
import com.sequenceiq.it.cloudbreak.cloud.v4.CommonCloudProperties;
import com.sequenceiq.it.cloudbreak.config.azure.AzureMarketplaceImageProperties;
import com.sequenceiq.it.cloudbreak.config.azure.ResourceGroupProperties;
import com.sequenceiq.it.cloudbreak.context.Clue;
import com.sequenceiq.it.cloudbreak.context.Investigable;
import com.sequenceiq.it.cloudbreak.context.RunningParameter;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.CloudbreakTestDto;
import com.sequenceiq.it.cloudbreak.dto.DeletableEnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.credential.CredentialTestDto;
import com.sequenceiq.it.cloudbreak.dto.telemetry.TelemetryTestDto;
import com.sequenceiq.it.cloudbreak.microservice.EnvironmentClient;
import com.sequenceiq.it.cloudbreak.search.Searchable;
import com.sequenceiq.it.cloudbreak.util.StructuredEventUtil;

@Prototype
public class EnvironmentTestDto
        extends DeletableEnvironmentTestDto<EnvironmentRequest, DetailedEnvironmentResponse, EnvironmentTestDto, SimpleEnvironmentResponse>
        implements Searchable, Assignable, Investigable {

    public static final String ENVIRONMENT_RESOURCE_NAME = "environmentName";

    private static final int ORDER = 100;

    @Inject
    private EnvironmentTestClient environmentTestClient;

    @Inject
    private ResourceGroupProperties resourceGroupProperties;

    @Inject
    private AzureMarketplaceImageProperties azureMarketplaceImageProperties;

    private Collection<SimpleEnvironmentResponse> simpleResponses;

    private SimpleEnvironmentResponse simpleResponse;

    private EnvironmentChangeCredentialRequest enviornmentChangeCredentialRequest;

    private int order = ORDER;

    @Inject
    private CommonCloudProperties commonCloudProperties;

    public EnvironmentTestDto(TestContext testContext) {
        super(new EnvironmentRequest(), testContext);
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
                .withCredentialName(getTestContext().get(CredentialTestDto.class) == null ? null :
                        getTestContext().get(CredentialTestDto.class).getName())
                .withAuthentication(getTestContext().given(EnvironmentAuthenticationTestDto.class))
                .withCloudplatform(getCloudPlatform().toString())
                .withIdBrokerMappingSource(IdBrokerMappingSource.MOCK)
                .withResourceGroup(resourceGroupProperties.getResourceGroupUsage(), resourceGroupProperties.getResourceGroupName())
                .withNetwork()
                .withCloudStorageValidation(CloudStorageValidation.ENABLED)
                .withTunnel(getTestContext().getTunnel())
                .withImageValidationFreeIpaCatalogAndImageIfPresent()
                .withEnvironmentType(EnvironmentType.PUBLIC_CLOUD);
    }

    private EnvironmentTestDto withEnvironmentType(EnvironmentType environmentType) {
        getRequest().setEnvironmentType(environmentType.name());
        return this;
    }

    public EnvironmentTestDto withOneFreeIpaNode() {
        getRequest().getFreeIpa().setInstanceCountByGroup(1);
        return this;
    }

    public EnvironmentTestDto withFreeIpaNodes(int instanceCountByGroup) {
        getRequest().getFreeIpa().setInstanceCountByGroup(instanceCountByGroup);
        return this;
    }

    public EnvironmentTestDto withCreateFreeIpa(Boolean create) {
        getRequest().getFreeIpa().setCreate(create);
        return this;
    }

    public EnvironmentTestDto withFreeIpaRecipe(Set<String> recipes) {
        getRequest().getFreeIpa().setRecipes(recipes);
        return this;
    }

    public EnvironmentTestDto withFreeIpaArchitecture(Architecture architecture) {
        if (architecture != null) {
            getRequest().getFreeIpa().setArchitecture(architecture.getName());
        }
        return this;
    }

    public EnvironmentTestDto withFreeIpaSeLinux(SeLinux seLinux) {
        if (seLinux != null) {
            if (getRequest().getFreeIpa().getSecurity() == null) {
                FreeIpaSecurityRequest freeIpaSecurityRequest = new FreeIpaSecurityRequest();
                freeIpaSecurityRequest.setSeLinux(seLinux.name());
                getRequest().getFreeIpa().setSecurity(freeIpaSecurityRequest);
            } else {
                getRequest().getFreeIpa().getSecurity().setSeLinux(seLinux.name());
            }
        }
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

    public EnvironmentTestDto withFreeIpaPlatformVariant(String variant) {
        getRequest().getFreeIpa().setPlatformVariant(variant);
        return this;

    }

    public EnvironmentTestDto withImageValidationFreeIpaCatalogAndImageIfPresent() {
        return withFreeIpaImage(commonCloudProperties.getImageValidation().getFreeIpaImageCatalog(),
                commonCloudProperties.getImageValidation().getFreeIpaImageUuid());
    }

    public EnvironmentTestDto withFreeIpaOs(String os) {
        if (!Strings.isNullOrEmpty(os)) {
            FreeIpaImageRequest imageRequest = new FreeIpaImageRequest();
            imageRequest.setOs(os);
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

    public EnvironmentTestDto withMarketplaceUpgradeFreeIpaImage() {
        FreeIpaImageRequest imageRequest = new FreeIpaImageRequest();
        imageRequest.setCatalog(getCloudProvider().getFreeIpaMarketplaceUpgradeImageCatalog());
        imageRequest.setId(getCloudProvider().getFreeIpaMarketplaceUpgradeImageId());
        getRequest().getFreeIpa().setImage(imageRequest);
        return this;
    }

    public EnvironmentTestDto withEnableMultiAzFreeIpa() {
        getRequest().getFreeIpa().setEnableMultiAz(true);
        return this;
    }

    public EnvironmentTestDto withDockerRegistryConfig(String crn) {
        DataServicesRequest dataServices = getRequest().getDataServices();
        if (dataServices == null) {
            dataServices = new DataServicesRequest();
        }
        CustomDockerRegistryV1Parameters customDockerRegistryV1Parameters = new CustomDockerRegistryV1Parameters();
        customDockerRegistryV1Parameters.setCrn(crn);
        dataServices.setCustomDockerRegistry(customDockerRegistryV1Parameters);
        getRequest().setDataServices(dataServices);
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

    public EnvironmentTestDto withTelemetryDisabled() {
        TelemetryRequest telemetry = new TelemetryRequest();
        FeaturesRequest features = new FeaturesRequest();
        FeatureSetting monitoringFeature = new FeatureSetting();
        monitoringFeature.setEnabled(false);
        features.setMonitoring(monitoringFeature);
        FeatureSetting workloadFeature = new FeatureSetting();
        workloadFeature.setEnabled(false);
        features.setWorkloadAnalytics(workloadFeature);
        FeatureSetting storageLoggingFeature = new FeatureSetting();
        storageLoggingFeature.setEnabled(false);
        features.setCloudStorageLogging(storageLoggingFeature);
        FeatureSetting clusterLogCollectionFeature = new FeatureSetting();
        clusterLogCollectionFeature.setEnabled(false);
        telemetry.setFeatures(features);
        getRequest().setTelemetry(telemetry);
        return this;
    }

    public EnvironmentTestDto withExternalizedComputeCluster(Set<String> subnets) {
        ExternalizedComputeCreateRequest externalizedComputeCreateRequest = getExternalizedComputeCreateRequest();
        externalizedComputeCreateRequest.setWorkerNodeSubnetIds(subnets);
        getRequest().setExternalizedComputeCreateRequest(externalizedComputeCreateRequest);
        return this;
    }

    public EnvironmentTestDto withExternalizedComputeCluster() {
        ExternalizedComputeCreateRequest externalizedComputeCreateRequest = getExternalizedComputeCreateRequest();
        getRequest().setExternalizedComputeCreateRequest(externalizedComputeCreateRequest);
        return this;
    }

    private ExternalizedComputeCreateRequest getExternalizedComputeCreateRequest() {
        ExternalizedComputeCreateRequest externalizedComputeCreateRequest = new ExternalizedComputeCreateRequest();
        externalizedComputeCreateRequest.setCreate(true);
        return externalizedComputeCreateRequest;
    }

    public EnvironmentTestDto withNetwork() {
        EnvironmentNetworkTestDto environmentNetwork = getCloudProvider().network(given(EnvironmentNetworkTestDto.class));
        if (environmentNetwork == null) {
            throw new IllegalArgumentException("Environment Network does not exist!");
        }
        return withNetwork(environmentNetwork.getRequest());
    }

    public EnvironmentTestDto withTrustSetup() {
        getRequest().setEnvironmentType("HYBRID");
        EnvironmentNetworkTestDto environmentNetwork = getCloudProvider().trustSetupNetwork(given(EnvironmentNetworkTestDto.class));
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

    @Deprecated
    public EnvironmentTestDto withNewNetwork() {
        EnvironmentNetworkTestDto environmentNetwork = getCloudProvider().newNetwork(given(EnvironmentNetworkTestDto.class));
        return withNetwork(environmentNetwork.getRequest());
    }

    public EnvironmentTestDto withResourceGroup(String resourceGroupUsage, String resourceGroupName) {
        return getCloudProvider().withResourceGroup(this, resourceGroupUsage, resourceGroupName);
    }

    public EnvironmentTestDto withResourceEncryption(Boolean resourceEncryptionEnabled) {
        if (resourceEncryptionEnabled) {
            return getCloudProvider().withResourceEncryption(this);
        }
        return this;
    }

    public EnvironmentTestDto withDatabaseEncryptionKey() {
        return getCloudProvider().withDatabaseEncryptionKey(this);
    }

    public EnvironmentTestDto withResourceEncryptionUserManagedIdentity() {
        return getCloudProvider().withResourceEncryptionUserManagedIdentity(this);
    }

    public EnvironmentTestDto withAws(AwsEnvironmentParameters awsEnvironmentParameters) {
        getRequest().setAws(awsEnvironmentParameters);
        return this;
    }

    public EnvironmentTestDto withFreeIpaAws(AwsFreeIpaParameters awsEnvironmentParameters) {
        getRequest().getFreeIpa().setAws(awsEnvironmentParameters);
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
        return new ArrayList<>(environmentEndpoint.list(null).getResponses()).stream()
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
        return when(environmentTestClient.refresh(), key("refresh-environment-" + getName()).withSkipOnFail(false).withLogError(false));
    }

    @Override
    public void deleteForCleanup() {
        try {
            EnvironmentClient client = getClientForCleanup();
            LOGGER.info("Deleting Environment with crn: {}", getCrn());
            client.getDefaultClient().environmentV1Endpoint().deleteByCrn(getCrn(), true, getCloudProvider().getGovCloud());
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

    private String getMarketplaceFreeIpaCatalogUrl() {
        return azureMarketplaceImageProperties.getCatalogUrl();
    }

    private String getMarketplaceFreeIpaImageUuid() {
        return azureMarketplaceImageProperties.getImageUuid();
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
        List<CDPStructuredEvent> structuredEvents = List.of();
        if (getResponse() != null && getResponse().getCrn() != null) {
            CDPStructuredEventV1Endpoint cdpStructuredEventV1Endpoint =
                    getTestContext().getMicroserviceClient(EnvironmentClient.class).getDefaultClient().structuredEventsV1Endpoint();
            structuredEvents = StructuredEventUtil.getAuditEvents(cdpStructuredEventV1Endpoint, getResponse().getCrn());
        }
        List<Searchable> listOfSearchables = List.of(this);
        return new Clue(
                getResponse().getName(),
                getResponse().getCrn(),
                null,
                getLogSearchUrl(listOfSearchables),
                null,
                structuredEvents,
                getResponse(),
                false);
    }

    public void setLastKnownFlow(FlowIdentifier flowIdentifier) {
        if (flowIdentifier.getType() == FlowType.FLOW) {
            setLastKnownFlowId(flowIdentifier.getPollableId());
        } else if (flowIdentifier.getType() == FlowType.FLOW_CHAIN) {
            setLastKnownFlowChainId(flowIdentifier.getPollableId());
        }
    }

    public EnvironmentTestDto withOverrideTunnel() {
        getRequest().setOverrideTunnel(Boolean.TRUE);
        return this;
    }

    public EnvironmentTestDto withoutOverrideTunnel() {
        getRequest().setOverrideTunnel(Boolean.FALSE);
        return this;
    }
}
