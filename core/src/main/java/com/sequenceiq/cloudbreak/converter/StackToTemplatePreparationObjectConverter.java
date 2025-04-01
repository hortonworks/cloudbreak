package com.sequenceiq.cloudbreak.converter;

import static com.sequenceiq.cloudbreak.common.type.CloudConstants.AWS;
import static com.sequenceiq.cloudbreak.common.type.CloudConstants.AZURE;
import static com.sequenceiq.cloudbreak.common.type.CloudConstants.GCP;
import static com.sequenceiq.cloudbreak.util.EphemeralVolumeUtil.getEphemeralVolumeWhichMustBeProvisioned;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.usermanagement.UserManagementProto;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.service.ExposedServiceCollector;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient;
import com.sequenceiq.cloudbreak.auth.altus.UmsVirtualGroupRight;
import com.sequenceiq.cloudbreak.auth.altus.VirtualGroupRequest;
import com.sequenceiq.cloudbreak.auth.altus.VirtualGroupService;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerRepo;
import com.sequenceiq.cloudbreak.cloud.model.StackInputs;
import com.sequenceiq.cloudbreak.cloud.model.StackTags;
import com.sequenceiq.cloudbreak.cluster.service.ClusterComponentConfigProvider;
import com.sequenceiq.cloudbreak.cmtemplate.cloudstorage.CmCloudStorageConfigProvider;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.ranger.RangerCloudStorageServiceConfigProvider;
import com.sequenceiq.cloudbreak.cmtemplate.general.GeneralClusterConfigsProvider;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.converter.spi.CredentialToCloudCredentialConverter;
import com.sequenceiq.cloudbreak.core.bootstrap.service.container.postgres.PostgresConfigService;
import com.sequenceiq.cloudbreak.domain.CustomConfigurations;
import com.sequenceiq.cloudbreak.domain.FileSystem;
import com.sequenceiq.cloudbreak.domain.StorageLocation;
import com.sequenceiq.cloudbreak.domain.cloudstorage.AccountMapping;
import com.sequenceiq.cloudbreak.domain.stack.cluster.IdBroker;
import com.sequenceiq.cloudbreak.domain.view.RdsConfigWithoutCluster;
import com.sequenceiq.cloudbreak.dto.LdapView;
import com.sequenceiq.cloudbreak.dto.StackDtoDelegate;
import com.sequenceiq.cloudbreak.dto.credential.Credential;
import com.sequenceiq.cloudbreak.kerberos.KerberosConfigService;
import com.sequenceiq.cloudbreak.ldap.LdapConfigService;
import com.sequenceiq.cloudbreak.sdx.RdcView;
import com.sequenceiq.cloudbreak.sdx.common.PlatformAwareSdxConnector;
import com.sequenceiq.cloudbreak.sdx.common.model.SdxBasicView;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.ServiceEndpointCollector;
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintViewProvider;
import com.sequenceiq.cloudbreak.service.cluster.DatabaseSslService;
import com.sequenceiq.cloudbreak.service.customconfigs.CustomConfigurationsService;
import com.sequenceiq.cloudbreak.service.customconfigs.CustomConfigurationsViewProvider;
import com.sequenceiq.cloudbreak.service.environment.EnvironmentClientService;
import com.sequenceiq.cloudbreak.service.environment.credential.CredentialConverter;
import com.sequenceiq.cloudbreak.service.hostgroup.HostGroupService;
import com.sequenceiq.cloudbreak.service.idbroker.IdBrokerService;
import com.sequenceiq.cloudbreak.service.identitymapping.AwsMockAccountMappingService;
import com.sequenceiq.cloudbreak.service.identitymapping.AzureMockAccountMappingService;
import com.sequenceiq.cloudbreak.service.identitymapping.GcpMockAccountMappingService;
import com.sequenceiq.cloudbreak.service.loadbalancer.LoadBalancerFqdnUtil;
import com.sequenceiq.cloudbreak.service.rdsconfig.RedbeamsDbServerConfigurer;
import com.sequenceiq.cloudbreak.service.secret.domain.Secret;
import com.sequenceiq.cloudbreak.service.sharedservice.DatalakeService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.tag.AccountTagValidationFailed;
import com.sequenceiq.cloudbreak.template.BlueprintProcessingException;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject.Builder;
import com.sequenceiq.cloudbreak.template.filesystem.BaseFileSystemConfigurationsView;
import com.sequenceiq.cloudbreak.template.filesystem.FileSystemConfigurationProvider;
import com.sequenceiq.cloudbreak.template.filesystem.StorageLocationView;
import com.sequenceiq.cloudbreak.template.model.GeneralClusterConfigs;
import com.sequenceiq.cloudbreak.template.views.AccountMappingView;
import com.sequenceiq.cloudbreak.template.views.BlueprintView;
import com.sequenceiq.cloudbreak.template.views.ClusterExposedServiceView;
import com.sequenceiq.cloudbreak.template.views.CustomConfigurationsView;
import com.sequenceiq.cloudbreak.template.views.DatabusCredentialView;
import com.sequenceiq.cloudbreak.template.views.DatalakeView;
import com.sequenceiq.cloudbreak.template.views.PlacementView;
import com.sequenceiq.cloudbreak.template.views.RdsView;
import com.sequenceiq.cloudbreak.template.views.provider.RdsViewProvider;
import com.sequenceiq.cloudbreak.view.ClusterView;
import com.sequenceiq.cloudbreak.view.GatewayView;
import com.sequenceiq.cloudbreak.view.InstanceGroupView;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.common.api.backup.response.BackupResponse;
import com.sequenceiq.common.api.telemetry.model.DataBusCredential;
import com.sequenceiq.common.api.telemetry.response.TelemetryResponse;
import com.sequenceiq.common.api.type.ResourceType;
import com.sequenceiq.environment.api.v1.environment.model.base.IdBrokerMappingSource;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;

@Component
public class StackToTemplatePreparationObjectConverter {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackToTemplatePreparationObjectConverter.class);

    @Inject
    private HostGroupService hostGroupService;

    @Inject
    private ClusterComponentConfigProvider clusterComponentConfigProvider;

    @Inject
    private PostgresConfigService postgresConfigService;

    @Inject
    private DatabaseSslService databaseSslService;

    @Inject
    private FileSystemConfigurationProvider fileSystemConfigurationProvider;

    @Inject
    private GeneralClusterConfigsProvider generalClusterConfigsProvider;

    @Inject
    private CustomConfigurationsViewProvider customConfigurationsViewProvider;

    @Inject
    private BlueprintViewProvider blueprintViewProvider;

    @Inject
    private CredentialConverter credentialConverter;

    @Inject
    private LdapConfigService ldapConfigService;

    @Inject
    private KerberosConfigService kerberosConfigService;

    @Inject
    private EnvironmentClientService environmentClientService;

    @Inject
    private AwsMockAccountMappingService awsMockAccountMappingService;

    @Inject
    private AzureMockAccountMappingService azureMockAccountMappingService;

    @Inject
    private GcpMockAccountMappingService gcpMockAccountMappingService;

    @Inject
    private CmCloudStorageConfigProvider cmCloudStorageConfigProvider;

    @Inject
    private ServiceEndpointCollector serviceEndpointCollector;

    @Inject
    private VirtualGroupService virtualGroupService;

    @Inject
    private EntitlementService entitlementService;

    @Inject
    private StackDtoService stackDtoService;

    @Inject
    private ExposedServiceCollector exposedServiceCollector;

    @Inject
    private GatewayConfigService gatewayConfigService;

    @Inject
    private PlatformAwareSdxConnector platformAwareSdxConnector;

    @Inject
    private IdBrokerService idBrokerService;

    @Inject
    private IdBrokerConverterUtil idBrokerConverterUtil;

    @Inject
    private GrpcUmsClient grpcUmsClient;

    @Inject
    private LoadBalancerFqdnUtil loadBalancerFqdnUtil;

    @Inject
    private TransactionService transactionService;

    @Inject
    private CredentialToCloudCredentialConverter credentialToCloudCredentialConverter;

    @Inject
    private DatalakeService datalakeService;

    @Inject
    private CustomConfigurationsService customConfigurationsService;

    @Inject
    private RdsViewProvider rdsViewProvider;

    public TemplatePreparationObject convert(StackDtoDelegate source) {
        try {
            Map<String, Collection<ClusterExposedServiceView>> views = serviceEndpointCollector.prepareClusterExposedServicesViews(source);
            DetailedEnvironmentResponse environment = environmentClientService.getByCrn(source.getEnvironmentCrn());
            Credential credential = credentialConverter.convert(environment.getCredential());
            ClusterView cluster = source.getCluster();
            FileSystem fileSystem = cluster.getFileSystem();
            Optional<LdapView> ldapView = ldapConfigService.get(source.getEnvironmentCrn(), source.getName());
            ClouderaManagerRepo cm = clusterComponentConfigProvider.getClouderaManagerRepoDetails(cluster.getId());
            List<ClouderaManagerProduct> products = clusterComponentConfigProvider.getClouderaManagerProductDetails(cluster.getId());
            BaseFileSystemConfigurationsView fileSystemConfigurationView = getFileSystemConfigurationView(credential, source, fileSystem);
            updateFileSystemViewWithBackupLocation(environment, fileSystemConfigurationView);
            StackInputs stackInputs = getStackInputs(source.getStack());
            Map<String, Object> fixInputs = stackInputs.getFixInputs() == null ? new HashMap<>() : stackInputs.getFixInputs();
            fixInputs.putAll(stackInputs.getDatalakeInputs() == null ? new HashMap<>() : stackInputs.getDatalakeInputs());
            GatewayView gateway = source.getGateway();
            String gatewaySignKey = null;
            if (gateway != null) {
                gatewaySignKey = gateway.getSignKey();
            }
            IdBroker idbroker = idBrokerService.getByCluster(cluster.getId());
            if (idbroker == null) {
                idbroker = idBrokerConverterUtil.generateIdBrokerSignKeys(cluster.getId(), source.getWorkspace());
                idBrokerService.save(idbroker);
            }
            String envCrnForVirtualGroups = getEnvironmentCrnForVirtualGroups(environment);
            VirtualGroupRequest virtualGroupRequest = new VirtualGroupRequest(envCrnForVirtualGroups, ldapView.map(LdapView::getAdminGroup).orElse(""));
            String accountId = Crn.safeFromString(source.getResourceCrn()).getAccountId();
            List<UserManagementProto.ServicePrincipalCloudIdentities> servicePrincipalCloudIdentities =
                    grpcUmsClient.listServicePrincipalCloudIdentities(accountId,
                            source.getEnvironmentCrn());

            BlueprintView blueprintView = blueprintViewProvider.getBlueprintView(source.getBlueprint());
            Optional<String> version = Optional.ofNullable(blueprintView.getVersion());
            String sslCertsFilePath = databaseSslService.getSslCertsFilePath();
            Set<RdsConfigWithoutCluster> rdsConfigWithoutClusters = postgresConfigService.createRdsConfigIfNeeded(source);
            boolean externalDatabaseRequested = RedbeamsDbServerConfigurer.isRemoteDatabaseRequested(cluster.getDatabaseServerCrn());
            Set<RdsView> rdsViews = rdsConfigWithoutClusters.stream()
                    .map(e -> rdsViewProvider.getRdsView(e, sslCertsFilePath, environment.getCloudPlatform(), externalDatabaseRequested))
                    .collect(Collectors.toSet());
            Builder builder = Builder.builder()
                    .withCloudPlatform(CloudPlatform.valueOf(source.getCloudPlatform()))
                    .withPlatformVariant(source.getPlatformVariant())
                    .withRdsViews(rdsViews)
                    .withRdsSslCertificateFilePath(sslCertsFilePath)
                    .withGateway(gateway, gatewaySignKey, exposedServiceCollector.getAllKnoxExposed(version))
                    .withIdBroker(idbroker)
                    .withCustomConfigurationsView(getCustomConfigurationsView(source.getStack(), cluster))
                    .withCustomInputs(stackInputs.getCustomInputs() == null ? new HashMap<>() : stackInputs.getCustomInputs())
                    .withFixInputs(fixInputs)
                    .withBlueprintView(blueprintView)
                    .withFileSystemConfigurationView(fileSystemConfigurationView)
                    .withGeneralClusterConfigs(calculateGeneralClusterConfigs(source, credential))
                    .withLdapConfig(ldapView.orElse(null))
                    .withKerberosConfig(kerberosConfigService.get(source.getEnvironmentCrn(), source.getName()).orElse(null))
                    .withProductDetails(cm, products)
                    .withExposedServices(views)
                    .withDefaultTags(getStackTags(source.getStack()))
                    .withSharedServiceConfigs(datalakeService.createSharedServiceConfigsView(source.getCluster().getPassword(), source.getType(),
                            source.getEnvironmentCrn()))
                    .withStackType(source.getType())
                    .withVirtualGroupView(virtualGroupRequest)
                    .withEnableSecretEncryption(environment.isEnableSecretEncryption());

            transactionService.required(() -> {
                builder.withHostgroups(hostGroupService.getByCluster(cluster.getId()), getEphemeralVolumeWhichMustBeProvisioned());
            });

            decorateDatabusCredential(source.getCluster().getDatabusCredentialSecret(), builder);
            decorateBuilderWithPlacement(source.getStack(), builder);
            decorateBuilderWithAccountMapping(source, environment, credential, builder, virtualGroupRequest);
            decorateBuilderWithServicePrincipals(source, builder, servicePrincipalCloudIdentities);
            decorateDatalakeView(source, builder);

            return builder.build();
        } catch (AccountTagValidationFailed aTVF) {
            throw new CloudbreakServiceException(aTVF);
        } catch (BlueprintProcessingException | IOException | TransactionService.TransactionExecutionException e) {
            throw new CloudbreakServiceException(e.getMessage(), e);
        }
    }

    private void decorateDatabusCredential(Secret databusCredentialSecret, Builder builder) {
        DataBusCredential dataBusCredential = null;
        if (databusCredentialSecret != null) {
            dataBusCredential = convertOrReturnNull(databusCredentialSecret.getRaw(), DataBusCredential.class);
        }
        if (dataBusCredential != null) {
            builder.withDatabusCredentialView(new DatabusCredentialView(dataBusCredential));
        }
    }

    private Map<String, String> getStackTags(StackView source) throws IOException {
        Map<String, String> userDefinedTags = new HashMap<>();
        if (source.getTags() != null) {
            StackTags stackTags = source.getTags().get(StackTags.class);
            if (stackTags != null) {
                StackTags stackTag = source.getTags().get(StackTags.class);
                Map<String, String> userDefined = stackTag.getUserDefinedTags();
                Map<String, String> defaultTags = stackTag.getDefaultTags();
                Map<String, String> applicationTags = stackTag.getApplicationTags();
                if (applicationTags != null) {
                    userDefinedTags.putAll(applicationTags);
                }
                if (userDefined != null) {
                    userDefinedTags.putAll(userDefined);
                }
                if (defaultTags != null) {
                    userDefinedTags.putAll(defaultTags);
                }
            }
        }
        return userDefinedTags;
    }

    private String getEnvironmentCrnForVirtualGroups(DetailedEnvironmentResponse environment) {
        String envCrnForVirtualGroups = environment.getCrn();
        if (StringUtils.isNoneEmpty(environment.getParentEnvironmentCrn())) {
            envCrnForVirtualGroups = environment.getParentEnvironmentCrn();
        }
        return envCrnForVirtualGroups;
    }

    private BaseFileSystemConfigurationsView getFileSystemConfigurationView(Credential credential, StackDtoDelegate stackDto, FileSystem fileSystem)
            throws IOException {
        BaseFileSystemConfigurationsView fileSystemConfigurationView = null;
        if (stackDto.getCluster().getFileSystem() != null) {
            fileSystemConfigurationView = fileSystemConfigurationProvider.fileSystemConfiguration(fileSystem, stackDto.getStack(),
                    (ResourceType type) -> stackDto.getResources().stream().filter(r -> r.getResourceType() == type).collect(Collectors.toList()),
                    credential.getAttributes(),
                    cmCloudStorageConfigProvider.getConfigQueryEntries());
        }
        return fileSystemConfigurationView;
    }

    private StackInputs getStackInputs(StackView source) throws IOException {
        StackInputs stackInputs = source.getInputs().get(StackInputs.class);
        if (stackInputs == null) {
            stackInputs = new StackInputs(new HashMap<>(), new HashMap<>(), new HashMap<>());
        }
        return stackInputs;
    }

    private void decorateBuilderWithPlacement(StackView source, Builder builder) {
        String region = source.getRegion();
        String availabilityZone = source.getAvailabilityZone();
        builder.withPlacementView(new PlacementView(region, availabilityZone));
    }

    private CustomConfigurationsView getCustomConfigurationsView(StackView source, ClusterView cluster) {
        CustomConfigurationsView customConfigurationsView = null;
        if (StackType.WORKLOAD.equals(source.getType()) && cluster.getCustomConfigurations() != null) {
            CustomConfigurations customConfigurationsWithConfigurations = customConfigurationsService.getByCrn(cluster.getCustomConfigurations().getCrn());
            customConfigurationsView = customConfigurationsViewProvider.getCustomConfigurationsView(customConfigurationsWithConfigurations);
        }
        return customConfigurationsView;
    }

    private void decorateBuilderWithAccountMapping(StackDtoDelegate source, DetailedEnvironmentResponse environment, Credential credential, Builder builder,
            VirtualGroupRequest virtualGroupRequest) {
        if (source.getType() == StackType.DATALAKE) {
            AccountMapping accountMapping = isCloudStorageConfigured(source) ? source.getCluster().getFileSystem().getCloudStorage().getAccountMapping() : null;
            if (accountMapping != null) {
                builder.withAccountMappingView(new AccountMappingView(accountMapping.getGroupMappings(), accountMapping.getUserMappings()));
            } else if (environment.getIdBrokerMappingSource() == IdBrokerMappingSource.MOCK && source.getCluster().getFileSystem() != null) {
                Map<String, String> groupMappings;
                Map<String, String> userMappings;
                CloudCredential cloudCredential = credentialToCloudCredentialConverter.convert(credential);
                String virtualGroup = getMockVirtualGroup(virtualGroupRequest);
                switch (source.getCloudPlatform()) {
                    case AWS:
                        groupMappings = awsMockAccountMappingService.getGroupMappings(source.getRegion(), cloudCredential, virtualGroup);
                        userMappings = awsMockAccountMappingService.getUserMappings(source.getRegion(), cloudCredential);
                        break;
                    case AZURE:
                        groupMappings = azureMockAccountMappingService.getGroupMappings(AzureMockAccountMappingService.MSI_RESOURCE_GROUP_NAME,
                                cloudCredential, virtualGroup);
                        userMappings = azureMockAccountMappingService.getUserMappings(AzureMockAccountMappingService.MSI_RESOURCE_GROUP_NAME,
                                cloudCredential);
                        break;
                    case GCP:
                        groupMappings = gcpMockAccountMappingService.getGroupMappings(source.getRegion(), cloudCredential, virtualGroup);
                        userMappings = gcpMockAccountMappingService.getUserMappings(source.getRegion(), cloudCredential);
                        break;
                    default:
                        return;
                }
                builder.withAccountMappingView(new AccountMappingView(groupMappings, userMappings));
            }
        }
    }

    private void decorateDatalakeView(StackDtoDelegate source, Builder builder) {
        DatalakeView datalakeView = null;
        if (StringUtils.isNotEmpty(source.getEnvironmentCrn()) && StackType.WORKLOAD.equals(source.getType())) {
            Optional<SdxBasicView> datalakeOpt = platformAwareSdxConnector.getSdxBasicViewByEnvironmentCrn(source.getEnvironmentCrn());
            if (datalakeOpt.isPresent()) {
                SdxBasicView datalake = datalakeOpt.get();
                boolean externalDatabaseForDL = RedbeamsDbServerConfigurer.isRemoteDatabaseRequested(datalake.dbServerCrn());
                RdcView rdcView = platformAwareSdxConnector.getRdcView(datalake.crn());
                datalakeView = new DatalakeView(datalake.razEnabled(), datalake.crn(), externalDatabaseForDL, rdcView);
            }
        }
        builder.withDataLakeView(datalakeView);
    }

    private String getMockVirtualGroup(VirtualGroupRequest virtualGroupRequest) {
        return virtualGroupService.createOrGetVirtualGroup(virtualGroupRequest, UmsVirtualGroupRight.CLOUDER_MANAGER_ADMIN);
    }

    private boolean isCloudStorageConfigured(StackDtoDelegate source) {
        return source.getCluster().getFileSystem() != null && source.getCluster().getFileSystem().getCloudStorage() != null;
    }

    private GeneralClusterConfigs calculateGeneralClusterConfigs(StackDtoDelegate stack, Credential credential) {
        StackView source = stack.getStack();
        GeneralClusterConfigs generalClusterConfigs = generalClusterConfigsProvider.generalClusterConfigs(stack, credential);
        boolean allInstanceGroupsHaveMultiAz = stack.getInstanceGroupViews().stream()
                .allMatch(ig -> isInstanceGroupsHaveMultiAz(ig, stack));
        generalClusterConfigs.setMultiAzEnabled(allInstanceGroupsHaveMultiAz);
        if (stack.getPrimaryGatewayInstance() != null) {
            if (StringUtils.isBlank(generalClusterConfigs.getClusterManagerIp())) {
                String primaryGatewayIp = gatewayConfigService.getPrimaryGatewayIp(stack);
                generalClusterConfigs.setClusterManagerIp(primaryGatewayIp);
            }
            Optional<String> instanceDiscoveryFQDN = generalClusterConfigs.getPrimaryGatewayInstanceDiscoveryFQDN();
            if (instanceDiscoveryFQDN.isEmpty()) {
                generalClusterConfigs.setPrimaryGatewayInstanceDiscoveryFQDN(Optional.of(stack.getPrimaryGatewayInstance().getDiscoveryFQDN()));
                List<InstanceMetadataView> otherInstanceMetadata = new ArrayList<>(stack.getAllAvailableGatewayInstances());
                otherInstanceMetadata.remove(stack.getPrimaryGatewayInstance());
                generalClusterConfigs.setOtherGatewayInstancesDiscoveryFQDN(otherInstanceMetadata.stream()
                        .map(im -> im.getDiscoveryFQDN()).collect(Collectors.toSet()));
            }
        }
        generalClusterConfigs.setLoadBalancerGatewayFqdn(Optional.ofNullable(loadBalancerFqdnUtil.getLoadBalancerUserFacingFQDN(source.getId())));
        generalClusterConfigs.setAccountId(Optional.ofNullable(Crn.safeFromString(source.getResourceCrn()).getAccountId()));
        generalClusterConfigs.setGovCloud(credential.isGovCloud());
        return generalClusterConfigs;
    }

    boolean isInstanceGroupsHaveMultiAz(InstanceGroupView instanceGroup, StackDtoDelegate stackDto) {
        Set<String> availabilityZonesByInstanceGroup = stackDto.getAvailabilityZonesByInstanceGroup(instanceGroup.getId());
        return availabilityZonesByInstanceGroup.size() > 1;
    }

    private void decorateBuilderWithServicePrincipals(StackDtoDelegate stackDto, Builder builder,
            List<UserManagementProto.ServicePrincipalCloudIdentities> servicePrincipalCloudIdentities) {
        StackView source = stackDto.getStack();
        if (StackType.DATALAKE.equals(source.getType())
                && AZURE.equals(source.getCloudPlatform())
                && stackDto.getCluster().isRangerRazEnabled()
                && entitlementService.cloudIdentityMappingEnabled(Crn.safeFromString(source.getResourceCrn()).getAccountId())) {

            ImmutableMap.Builder<String, String> azureObjectIdMap = ImmutableMap.builder();
            servicePrincipalCloudIdentities.forEach(spCloudId -> {
                Optional<String> azureObjectId = getOptionalAzureObjectId(spCloudId.getCloudIdentitiesList());
                if (azureObjectId.isPresent()) {
                    azureObjectIdMap.put(spCloudId.getServicePrincipal(), azureObjectId.get());
                }
            });

            builder.withServicePrincipals(azureObjectIdMap.build());
        } else {
            builder.withServicePrincipals(null);
        }
    }

    private Optional<String> getOptionalAzureObjectId(List<UserManagementProto.CloudIdentity> cloudIdentities) {
        List<UserManagementProto.CloudIdentity> azureCloudIdentities = cloudIdentities.stream()
                .filter(cloudIdentity -> cloudIdentity.getCloudIdentityName().hasAzureCloudIdentityName())
                .collect(Collectors.toList());
        if (azureCloudIdentities.isEmpty()) {
            return Optional.empty();
        } else if (azureCloudIdentities.size() > 1) {
            throw new IllegalStateException(String.format("List contains multiple azure cloud identities = %s", cloudIdentities));
        } else {
            String azureObjectId = Iterables.getOnlyElement(azureCloudIdentities).getCloudIdentityName().getAzureCloudIdentityName().getObjectId();
            return Optional.of(azureObjectId);
        }
    }

    private void updateFileSystemViewWithBackupLocation(DetailedEnvironmentResponse detailedEnvironmentResponse,
            BaseFileSystemConfigurationsView fileSystemConfigurationView) {
        if (fileSystemConfigurationView != null) {
            BackupResponse backupResponse = detailedEnvironmentResponse.getBackup();
            TelemetryResponse telemetryResponse = detailedEnvironmentResponse.getTelemetry();
            Optional<String> backupLocation = Optional.empty();
            if (backupResponse != null && backupResponse.getStorageLocation() != null) {
                backupLocation = Optional.of(backupResponse.getStorageLocation());
            } else if (telemetryResponse != null && telemetryResponse.getLogging() != null) {
                backupLocation = Optional.of(telemetryResponse.getLogging().getStorageLocation());
            }

            if (backupLocation.isPresent()) {
                StorageLocation storageLocation = new StorageLocation();
                storageLocation.setValue(backupLocation.get());
                storageLocation.setProperty(RangerCloudStorageServiceConfigProvider.DEFAULT_BACKUP_DIR);
                StorageLocationView backupLocationView = new StorageLocationView(storageLocation);
                fileSystemConfigurationView.getLocations().add(backupLocationView);
            }
        }
    }

    private <T> T convertOrReturnNull(String value, Class<T> type) {
        if (StringUtils.isNotBlank(value)) {
            try {
                return new Json(value).get(type);
            } catch (IOException e) {
                LOGGER.error("Cannot read {} from cluster entity. Continue without value.", type.getSimpleName(), e);
            }
        }
        return null;
    }
}
