package com.sequenceiq.cloudbreak.converter.v4.stacks;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.sharedservice.SharedServiceV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.environment.placement.PlacementSettingsV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.InstanceGroupV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.template.volume.VolumeV4Request;
import com.sequenceiq.cloudbreak.blueprint.GeneralClusterConfigsProvider;
import com.sequenceiq.cloudbreak.blueprint.utils.StackInfoService;
import com.sequenceiq.cloudbreak.cmtemplate.cloudstorage.CmCloudStorageConfigProvider;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.user.CloudbreakUser;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.converter.util.CloudStorageValidationUtil;
import com.sequenceiq.cloudbreak.converter.v4.stacks.cluster.CloudStorageConverter;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.FileSystem;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.domain.stack.cluster.DatalakeResources;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.Gateway;
import com.sequenceiq.cloudbreak.dto.KerberosConfig;
import com.sequenceiq.cloudbreak.dto.LdapView;
import com.sequenceiq.cloudbreak.dto.credential.Credential;
import com.sequenceiq.cloudbreak.kerberos.KerberosConfigService;
import com.sequenceiq.cloudbreak.ldap.LdapConfigService;
import com.sequenceiq.cloudbreak.service.CloudbreakRestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintService;
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintViewProvider;
import com.sequenceiq.cloudbreak.service.datalake.DatalakeResourcesService;
import com.sequenceiq.cloudbreak.service.environment.EnvironmentClientService;
import com.sequenceiq.cloudbreak.service.environment.credential.CredentialClientService;
import com.sequenceiq.cloudbreak.service.environment.credential.CredentialConverter;
import com.sequenceiq.cloudbreak.service.identitymapping.AwsMockAccountMappingService;
import com.sequenceiq.cloudbreak.service.identitymapping.AzureMockAccountMappingService;
import com.sequenceiq.cloudbreak.service.rdsconfig.RdsConfigService;
import com.sequenceiq.cloudbreak.service.sharedservice.AmbariDatalakeConfigProvider;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.user.UserService;
import com.sequenceiq.cloudbreak.service.workspace.WorkspaceService;
import com.sequenceiq.cloudbreak.template.BlueprintProcessingException;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject.Builder;
import com.sequenceiq.cloudbreak.template.filesystem.BaseFileSystemConfigurationsView;
import com.sequenceiq.cloudbreak.template.filesystem.FileSystemConfigurationProvider;
import com.sequenceiq.cloudbreak.template.model.BlueprintStackInfo;
import com.sequenceiq.cloudbreak.template.model.GeneralClusterConfigs;
import com.sequenceiq.cloudbreak.template.views.AccountMappingView;
import com.sequenceiq.cloudbreak.template.views.BlueprintView;
import com.sequenceiq.cloudbreak.template.views.HostgroupView;
import com.sequenceiq.cloudbreak.template.views.PlacementView;
import com.sequenceiq.cloudbreak.template.views.SharedServiceConfigsView;
import com.sequenceiq.cloudbreak.workspace.model.User;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;
import com.sequenceiq.common.api.cloudstorage.AccountMappingBase;
import com.sequenceiq.environment.api.v1.environment.model.base.IdBrokerMappingSource;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;

@Component
public class StackV4RequestToTemplatePreparationObjectConverter extends AbstractConversionServiceAwareConverter<StackV4Request, TemplatePreparationObject> {

    @Inject
    private LdapConfigService ldapConfigService;

    @Inject
    private RdsConfigService rdsConfigService;

    @Inject
    private GeneralClusterConfigsProvider generalClusterConfigsProvider;

    @Inject
    private BlueprintService blueprintService;

    @Inject
    private CredentialClientService credentialClientService;

    @Inject
    private FileSystemConfigurationProvider fileSystemConfigurationProvider;

    @Inject
    private StackInfoService stackInfoService;

    @Inject
    private CloudStorageValidationUtil cloudStorageValidationUtil;

    @Inject
    private UserService userService;

    @Inject
    private CloudbreakRestRequestThreadLocalService restRequestThreadLocalService;

    @Inject
    private WorkspaceService workspaceService;

    @Inject
    private EnvironmentClientService environmentClientService;

    @Inject
    private KerberosConfigService kerberosConfigService;

    @Inject
    private AmbariDatalakeConfigProvider ambariDatalakeConfigProvider;

    @Inject
    private DatalakeResourcesService datalakeResourcesService;

    @Inject
    private StackService stackService;

    @Inject
    private BlueprintViewProvider blueprintViewProvider;

    @Inject
    private CredentialConverter credentialConverter;

    @Inject
    private AwsMockAccountMappingService awsMockAccountMappingService;

    @Inject
    private AzureMockAccountMappingService azureMockAccountMappingService;

    @Inject
    private CloudStorageConverter cloudStorageConverter;

    @Inject
    private CmCloudStorageConfigProvider cmCloudStorageConfigProvider;

    @Override
    public TemplatePreparationObject convert(StackV4Request source) {
        try {
            CloudbreakUser cloudbreakUser = restRequestThreadLocalService.getCloudbreakUser();
            User user = userService.getOrCreate(cloudbreakUser);
            Workspace workspace = workspaceService.get(restRequestThreadLocalService.getRequestedWorkspaceId(), user);
            DetailedEnvironmentResponse environment = environmentClientService.getByCrn(source.getEnvironmentCrn());
            Credential credential = getCredential(source, environment);
            LdapView ldapConfig = getLdapConfig(source);
            BaseFileSystemConfigurationsView fileSystemConfigurationView = getFileSystemConfigurationView(source, credential.getAttributes());
            Set<RDSConfig> rdsConfigs = getRdsConfigs(source, workspace);
            Blueprint blueprint = getBlueprint(source, workspace);
            String blueprintText = blueprint.getBlueprintText();
            BlueprintStackInfo blueprintStackInfo = stackInfoService.blueprintStackInfo(blueprintText);
            Set<HostgroupView> hostgroupViews = getHostgroupViews(source);
            Gateway gateway = source.getCluster().getGateway() == null ? null : getConversionService().convert(source, Gateway.class);
            BlueprintView blueprintView = blueprintViewProvider.getBlueprintView(blueprint);
            GeneralClusterConfigs generalClusterConfigs = generalClusterConfigsProvider.generalClusterConfigs(source, cloudbreakUser.getEmail(),
                    blueprintService.getBlueprintVariant(blueprint));
            String gatewaySignKey = null;
            if (gateway != null) {
                gatewaySignKey = gateway.getSignKey();
            }
            Builder builder = Builder.builder()
                    .withCloudPlatform(source.getCloudPlatform())
                    .withRdsConfigs(rdsConfigs)
                    .withHostgroupViews(hostgroupViews)
                    .withGateway(gateway, gatewaySignKey)
                    .withBlueprintView(blueprintView)
                    .withStackRepoDetailsHdpVersion(blueprintStackInfo.getVersion())
                    .withFileSystemConfigurationView(fileSystemConfigurationView)
                    .withGeneralClusterConfigs(generalClusterConfigs)
                    .withLdapConfig(ldapConfig)
                    .withCustomInputs(source.getInputs())
                    .withKerberosConfig(getKerberosConfig(source))
                    .withStackType(source.getType());

            SharedServiceV4Request sharedService = source.getSharedService();
            if (sharedService != null && StringUtils.isNotBlank(sharedService.getDatalakeName())) {
                DatalakeResources datalakeResource = datalakeResourcesService.getByNameForWorkspace(source.getSharedService().getDatalakeName(), workspace);
                if (datalakeResource != null) {
                    SharedServiceConfigsView sharedServiceConfigsView = ambariDatalakeConfigProvider.createSharedServiceConfigView(datalakeResource);
                    Map<String, String> additionalParams = ambariDatalakeConfigProvider.getAdditionalParameters(source, datalakeResource);
                    builder.withSharedServiceConfigs(sharedServiceConfigsView)
                            .withFixInputs((Map) additionalParams);
                } else {
                    throw new CloudbreakServiceException("Cannot collect shared service resources from datalake!");
                }
            }
            decorateBuilderWithPlacement(source, builder);
            decorateBuilderWithAccountMapping(source, environment, credential, builder);

            return builder.build();
        } catch (BlueprintProcessingException | IOException e) {
            throw new CloudbreakServiceException(e.getMessage(), e);
        }
    }

    private Credential getCredential(StackV4Request source, DetailedEnvironmentResponse environment) {
        return credentialConverter.convert(environment.getCredential());
    }

    private Blueprint getBlueprint(StackV4Request source, Workspace workspace) {
        return blueprintService.getByNameForWorkspace(source.getCluster().getBlueprintName(), workspace);
    }

    private Set<HostgroupView> getHostgroupViews(StackV4Request source) {
        Set<HostgroupView> hostgroupViews = new HashSet<>();
        for (InstanceGroupV4Request instanceGroup : source.getInstanceGroups()) {
            hostgroupViews.add(
                    new HostgroupView(
                            instanceGroup.getName(),
                            instanceGroup.getTemplate().getAttachedVolumes().stream().mapToInt(VolumeV4Request::getCount).sum(),
                            instanceGroup.getType(),
                            instanceGroup.getNodeCount()));
        }
        return hostgroupViews;
    }

    private Set<RDSConfig> getRdsConfigs(StackV4Request source, Workspace workspace) {
        return source.getCluster().getDatabases().stream()
                .map(d -> rdsConfigService.getByNameForWorkspace(d, workspace))
                .collect(Collectors.toSet());
    }

    private BaseFileSystemConfigurationsView getFileSystemConfigurationView(StackV4Request source, Json credentialAttributes)
            throws IOException {
        BaseFileSystemConfigurationsView fileSystemConfigurationView = null;
        if (isCloudStorageConfigured(source)) {
            FileSystem fileSystem = cloudStorageConverter.requestToFileSystem(source.getCluster().getCloudStorage());
            fileSystemConfigurationView = fileSystemConfigurationProvider.fileSystemConfiguration(fileSystem, source, credentialAttributes,
                    cmCloudStorageConfigProvider.getConfigQueryEntries());
        }
        return fileSystemConfigurationView;
    }

    private LdapView getLdapConfig(StackV4Request source) {
        return ldapConfigService.get(source.getEnvironmentCrn(), source.getName()).orElse(null);
    }

    private KerberosConfig getKerberosConfig(StackV4Request source) {
        return kerberosConfigService.get(source.getEnvironmentCrn(), source.getName()).orElse(null);
    }

    private void decorateBuilderWithPlacement(StackV4Request source, Builder builder) {
        PlacementSettingsV4Request placementSettings = source.getPlacement();
        if (placementSettings != null) {
            String region = placementSettings.getRegion();
            String availabilityZone = placementSettings.getAvailabilityZone();
            builder.withPlacementView(new PlacementView(region, availabilityZone));
        }
    }

    private void decorateBuilderWithAccountMapping(StackV4Request source, DetailedEnvironmentResponse environment, Credential credential, Builder builder) {
        if (source.getType() == StackType.DATALAKE) {
            AccountMappingBase accountMapping = isCloudStorageConfigured(source) ? source.getCluster().getCloudStorage().getAccountMapping() : null;
            if (accountMapping != null) {
                builder.withAccountMappingView(new AccountMappingView(accountMapping.getGroupMappings(), accountMapping.getUserMappings()));
            } else if (environment.getIdBrokerMappingSource() == IdBrokerMappingSource.MOCK) {
                Map<String, String> groupMappings;
                Map<String, String> userMappings;
                switch (source.getCloudPlatform()) {
                    case AWS:
                        groupMappings = awsMockAccountMappingService.getGroupMappings(source.getPlacement().getRegion(), credential,
                                environment.getAdminGroupName());
                        userMappings = awsMockAccountMappingService.getUserMappings(source.getPlacement().getRegion(), credential);
                        break;
                    case AZURE:
                        groupMappings = azureMockAccountMappingService.getGroupMappings(AzureMockAccountMappingService.MSI_RESOURCE_GROUP_NAME,
                                credential,
                                environment.getAdminGroupName());
                        userMappings = azureMockAccountMappingService.getUserMappings(AzureMockAccountMappingService.MSI_RESOURCE_GROUP_NAME,
                                credential);
                        break;
                    default:
                        return;
                }
                builder.withAccountMappingView(new AccountMappingView(groupMappings, userMappings));
            }
        }
    }

    private boolean isCloudStorageConfigured(StackV4Request source) {
        return cloudStorageValidationUtil.isCloudStorageConfigured(source.getCluster().getCloudStorage());
    }

}
