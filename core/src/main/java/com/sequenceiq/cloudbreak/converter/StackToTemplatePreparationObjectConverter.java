package com.sequenceiq.cloudbreak.converter;

import static com.sequenceiq.cloudbreak.common.type.CloudConstants.AWS;
import static com.sequenceiq.cloudbreak.common.type.CloudConstants.AZURE;
import static com.sequenceiq.cloudbreak.common.type.CloudConstants.GCP;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.usermanagement.UserManagementProto;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.dto.NameOrCrn;
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
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.converter.spi.CredentialToCloudCredentialConverter;
import com.sequenceiq.cloudbreak.core.bootstrap.service.container.postgres.PostgresConfigService;
import com.sequenceiq.cloudbreak.domain.FileSystem;
import com.sequenceiq.cloudbreak.domain.StorageLocation;
import com.sequenceiq.cloudbreak.domain.cloudstorage.AccountMapping;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.IdBroker;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.Gateway;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.dto.LdapView;
import com.sequenceiq.cloudbreak.dto.credential.Credential;
import com.sequenceiq.cloudbreak.exception.CustomConfigurationsRuntimeVersionException;
import com.sequenceiq.cloudbreak.kerberos.KerberosConfigService;
import com.sequenceiq.cloudbreak.ldap.LdapConfigService;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.LoadBalancerConfigService;
import com.sequenceiq.cloudbreak.service.ServiceEndpointCollector;
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintViewProvider;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.customconfigs.CustomConfigurationsService;
import com.sequenceiq.cloudbreak.service.customconfigs.CustomConfigurationsViewProvider;
import com.sequenceiq.cloudbreak.service.datalake.SdxClientService;
import com.sequenceiq.cloudbreak.service.environment.EnvironmentClientService;
import com.sequenceiq.cloudbreak.service.environment.credential.CredentialConverter;
import com.sequenceiq.cloudbreak.service.hostgroup.HostGroupService;
import com.sequenceiq.cloudbreak.service.idbroker.IdBrokerService;
import com.sequenceiq.cloudbreak.service.identitymapping.AwsMockAccountMappingService;
import com.sequenceiq.cloudbreak.service.identitymapping.AzureMockAccountMappingService;
import com.sequenceiq.cloudbreak.service.identitymapping.GcpMockAccountMappingService;
import com.sequenceiq.cloudbreak.service.rdsconfig.RedbeamsDbCertificateProvider;
import com.sequenceiq.cloudbreak.service.resource.ResourceService;
import com.sequenceiq.cloudbreak.service.sharedservice.DatalakeService;
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
import com.sequenceiq.cloudbreak.template.views.DatalakeView;
import com.sequenceiq.cloudbreak.template.views.PlacementView;
import com.sequenceiq.cloudbreak.util.StackUtil;
import com.sequenceiq.common.api.backup.response.BackupResponse;
import com.sequenceiq.common.api.telemetry.response.TelemetryResponse;
import com.sequenceiq.common.api.type.ResourceType;
import com.sequenceiq.environment.api.v1.environment.model.base.IdBrokerMappingSource;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.sdx.api.model.SdxClusterResponse;

@Component
public class StackToTemplatePreparationObjectConverter {

    @Inject
    private HostGroupService hostGroupService;

    @Inject
    private ClusterComponentConfigProvider clusterComponentConfigProvider;

    @Inject
    private PostgresConfigService postgresConfigService;

    @Inject
    private RedbeamsDbCertificateProvider dbCertificateProvider;

    @Inject
    private FileSystemConfigurationProvider fileSystemConfigurationProvider;

    @Inject
    private ClusterService clusterService;

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
    private CustomConfigurationsService customConfigurationsService;

    @Inject
    private CmCloudStorageConfigProvider cmCloudStorageConfigProvider;

    @Inject
    private ServiceEndpointCollector serviceEndpointCollector;

    @Inject
    private StackUtil stackUtil;

    @Inject
    private VirtualGroupService virtualGroupService;

    @Inject
    private EntitlementService entitlementService;

    @Inject
    private ExposedServiceCollector exposedServiceCollector;

    @Inject
    private ResourceService resourceService;

    @Inject
    private GatewayConfigService gatewayConfigService;

    @Inject
    private SdxClientService sdxClientService;

    @Inject
    private IdBrokerService idBrokerService;

    @Inject
    private IdBrokerConverterUtil idBrokerConverterUtil;

    @Inject
    private GrpcUmsClient grpcUmsClient;

    @Inject
    private LoadBalancerConfigService loadBalancerConfigService;

    @Inject
    private TransactionService transactionService;

    @Inject
    private CredentialToCloudCredentialConverter credentialToCloudCredentialConverter;

    @Inject
    private DatalakeService datalakeService;

    public TemplatePreparationObject convert(Stack source) {
        try {
            Map<String, Collection<ClusterExposedServiceView>> views = serviceEndpointCollector
                    .prepareClusterExposedServicesViews(source.getCluster(),
                            stackUtil.extractClusterManagerAddress(source));
            DetailedEnvironmentResponse environment = environmentClientService.getByCrn(source.getEnvironmentCrn());
            Credential credential = credentialConverter.convert(environment.getCredential());
            Cluster cluster = clusterService.getById(source.getCluster().getId());
            FileSystem fileSystem = cluster.getFileSystem();
            Optional<LdapView> ldapView = ldapConfigService.get(source.getEnvironmentCrn(), source.getName());
            ClouderaManagerRepo cm = clusterComponentConfigProvider.getClouderaManagerRepoDetails(cluster.getId());
            List<ClouderaManagerProduct> products = clusterComponentConfigProvider.getClouderaManagerProductDetails(cluster.getId());
            BaseFileSystemConfigurationsView fileSystemConfigurationView = getFileSystemConfigurationView(credential, source, fileSystem);
            updateFileSystemViewWithBackupLocation(environment, fileSystemConfigurationView);
            StackInputs stackInputs = getStackInputs(source);
            Map<String, Object> fixInputs = stackInputs.getFixInputs() == null ? new HashMap<>() : stackInputs.getFixInputs();
            fixInputs.putAll(stackInputs.getDatalakeInputs() == null ? new HashMap<>() : stackInputs.getDatalakeInputs());
            Gateway gateway = cluster.getGateway();
            String gatewaySignKey = null;
            if (gateway != null) {
                gatewaySignKey = gateway.getSignKey();
            }
            IdBroker idbroker =  idBrokerService.getByCluster(cluster);
            if (idbroker == null) {
                idbroker = idBrokerConverterUtil.generateIdBrokerSignKeys(cluster);
                idBrokerService.save(idbroker);
            }
            String envCrnForVirtualGroups = getEnvironmentCrnForVirtualGroups(environment);
            VirtualGroupRequest virtualGroupRequest = new VirtualGroupRequest(envCrnForVirtualGroups, ldapView.map(LdapView::getAdminGroup).orElse(""));
            String accountId = Crn.safeFromString(source.getResourceCrn()).getAccountId();
            List<UserManagementProto.ServicePrincipalCloudIdentities> servicePrincipalCloudIdentities =
                    grpcUmsClient.listServicePrincipalCloudIdentities(accountId,
                            source.getEnvironmentCrn());

            BlueprintView blueprintView = blueprintViewProvider.getBlueprintView(cluster.getBlueprint());
            Optional<String> version = Optional.ofNullable(blueprintView.getVersion());

            Builder builder = Builder.builder()
                    .withCloudPlatform(CloudPlatform.valueOf(source.getCloudPlatform()))
                    .withRdsConfigs(postgresConfigService.createRdsConfigIfNeeded(source, cluster))
                    .withRdsSslCertificateFilePath(dbCertificateProvider.getSslCertsFilePath())
                    .withGateway(gateway, gatewaySignKey, exposedServiceCollector.getAllKnoxExposed(version))
                    .withIdBroker(idbroker)
                    .withCustomConfigurationsView(getCustomConfigurationsView(source, cluster))
                    .withCustomInputs(stackInputs.getCustomInputs() == null ? new HashMap<>() : stackInputs.getCustomInputs())
                    .withFixInputs(fixInputs)
                    .withBlueprintView(blueprintView)
                    .withFileSystemConfigurationView(fileSystemConfigurationView)
                    .withGeneralClusterConfigs(calculateGeneralClusterConfigs(source, cluster))
                    .withLdapConfig(ldapView.orElse(null))
                    .withKerberosConfig(kerberosConfigService.get(source.getEnvironmentCrn(), source.getName()).orElse(null))
                    .withProductDetails(cm, products)
                    .withExposedServices(views)
                    .withDefaultTags(getStackTags(source))
                    .withSharedServiceConfigs(datalakeService.createSharedServiceConfigsView(source))
                    .withStackType(source.getType())
                    .withVirtualGroupView(virtualGroupRequest);

            transactionService.required(() -> {
                builder.withHostgroups(hostGroupService.getByCluster(cluster.getId()));
            });

            decorateBuilderWithPlacement(source, builder);
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

    private Map<String, String> getStackTags(Stack source) throws IOException {
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

    private BaseFileSystemConfigurationsView getFileSystemConfigurationView(Credential credential, Stack source, FileSystem fileSystem) throws IOException {
        BaseFileSystemConfigurationsView fileSystemConfigurationView = null;
        if (source.getCluster().getFileSystem() != null) {
            fileSystemConfigurationView = fileSystemConfigurationProvider.fileSystemConfiguration(fileSystem, source,
                    (ResourceType r) -> resourceService.findByStackIdAndType(source.getId(), r), credential.getAttributes(),
                    cmCloudStorageConfigProvider.getConfigQueryEntries());
        }
        return fileSystemConfigurationView;
    }

    private StackInputs getStackInputs(Stack source) throws IOException {
        StackInputs stackInputs = source.getInputs().get(StackInputs.class);
        if (stackInputs == null) {
            stackInputs = new StackInputs(new HashMap<>(), new HashMap<>(), new HashMap<>());
        }
        return stackInputs;
    }

    private void decorateBuilderWithPlacement(Stack source, Builder builder) {
        String region = source.getRegion();
        String availabilityZone = source.getAvailabilityZone();
        builder.withPlacementView(new PlacementView(region, availabilityZone));
    }

    private CustomConfigurationsView getCustomConfigurationsView(Stack source, Cluster cluster) {
        CustomConfigurationsView customConfigurationsView = null;
        if (StackType.WORKLOAD.equals(source.getType()) && source.getCluster().getCustomConfigurations() != null) {
            customConfigurationsView = customConfigurationsViewProvider.getCustomConfigurationsView(customConfigurationsService
                    .getByNameOrCrn(NameOrCrn.ofCrn(clusterService.findOneWithCustomConfigurations(cluster.getId()).getCustomConfigurations().getCrn())));
            if (!Strings.isNullOrEmpty(customConfigurationsView.getRuntimeVersion()) &&
                    !source.getStackVersion().equals(customConfigurationsView.getRuntimeVersion())) {
                throw new CustomConfigurationsRuntimeVersionException("Custom Configurations runtime version mismatch!");
            }
        }
        return customConfigurationsView;
    }

    private void decorateBuilderWithAccountMapping(Stack source, DetailedEnvironmentResponse environment, Credential credential, Builder builder,
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

    private void decorateDatalakeView(Stack source, TemplatePreparationObject.Builder builder) {
        DatalakeView datalakeView = null;
        if (StringUtils.isNotEmpty(source.getEnvironmentCrn()) && StackType.WORKLOAD.equals(source.getType())) {
            List<SdxClusterResponse> datalakes = sdxClientService.getByEnvironmentCrn(source.getEnvironmentCrn());
            if (!datalakes.isEmpty()) {
                datalakeView = new DatalakeView(datalakes.get(0).getRangerRazEnabled());
            }
        }
        builder.withDataLakeView(datalakeView);
    }

    private String getMockVirtualGroup(VirtualGroupRequest virtualGroupRequest) {
        return virtualGroupService.createOrGetVirtualGroup(virtualGroupRequest, UmsVirtualGroupRight.CLOUDER_MANAGER_ADMIN);
    }

    private boolean isCloudStorageConfigured(Stack source) {
        return source.getCluster().getFileSystem() != null && source.getCluster().getFileSystem().getCloudStorage() != null;
    }

    private GeneralClusterConfigs calculateGeneralClusterConfigs(Stack source, Cluster cluster) {
        GeneralClusterConfigs generalClusterConfigs = generalClusterConfigsProvider.generalClusterConfigs(source, cluster);
        boolean allInstanceGroupsHaveMultiAz = source.getInstanceGroups().stream().allMatch(this::isInstanceGroupsHaveMultiAz);
        generalClusterConfigs.setMultiAzEnabled(allInstanceGroupsHaveMultiAz);
        if (source.getPrimaryGatewayInstance() != null) {
            if (StringUtils.isBlank(generalClusterConfigs.getClusterManagerIp())) {
                String primaryGatewayIp = gatewayConfigService.getPrimaryGatewayIp(source);
                generalClusterConfigs.setClusterManagerIp(primaryGatewayIp);
            }
            Optional<String> instanceDiscoveryFQDN = generalClusterConfigs.getPrimaryGatewayInstanceDiscoveryFQDN();
            if (instanceDiscoveryFQDN.isEmpty()) {
                generalClusterConfigs.setPrimaryGatewayInstanceDiscoveryFQDN(Optional.of(source.getPrimaryGatewayInstance().getDiscoveryFQDN()));
            }
        }
        generalClusterConfigs.setLoadBalancerGatewayFqdn(Optional.ofNullable(loadBalancerConfigService.getLoadBalancerUserFacingFQDN(source.getId())));
        generalClusterConfigs.setAccountId(Optional.ofNullable(Crn.safeFromString(source.getResourceCrn()).getAccountId()));
        return generalClusterConfigs;
    }

    boolean isInstanceGroupsHaveMultiAz(InstanceGroup instanceGroup) {
        return instanceGroup.getAvailabilityZones().size() > 1;
    }

    private void decorateBuilderWithServicePrincipals(Stack source, Builder builder,
            List<UserManagementProto.ServicePrincipalCloudIdentities> servicePrincipalCloudIdentities) {
        if (StackType.DATALAKE.equals(source.getType())
                && AZURE.equals(source.cloudPlatform())
                && source.getCluster().isRangerRazEnabled()
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
}
