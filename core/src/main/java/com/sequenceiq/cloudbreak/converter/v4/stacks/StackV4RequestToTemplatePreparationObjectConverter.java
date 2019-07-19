package com.sequenceiq.cloudbreak.converter.v4.stacks;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.sharedservice.SharedServiceV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.InstanceGroupV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.template.volume.VolumeV4Request;
import com.sequenceiq.cloudbreak.blueprint.GeneralClusterConfigsProvider;
import com.sequenceiq.cloudbreak.blueprint.utils.StackInfoService;
import com.sequenceiq.cloudbreak.cluster.api.DatalakeConfigApi;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.common.user.CloudbreakUser;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.converter.util.CloudStorageValidationUtil;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.FileSystem;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
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
import com.sequenceiq.cloudbreak.service.identitymapping.AwsIdentityMappingService;
import com.sequenceiq.cloudbreak.service.rdsconfig.RdsConfigService;
import com.sequenceiq.cloudbreak.service.sharedservice.AmbariDatalakeConfigProvider;
import com.sequenceiq.cloudbreak.service.sharedservice.DatalakeConfigApiConnector;
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
import com.sequenceiq.cloudbreak.template.views.BlueprintView;
import com.sequenceiq.cloudbreak.template.views.HostgroupView;
import com.sequenceiq.cloudbreak.template.views.SharedServiceConfigsView;
import com.sequenceiq.cloudbreak.workspace.model.User;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;
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
    private DatalakeConfigApiConnector datalakeConfigApiConnector;

    @Inject
    private BlueprintViewProvider blueprintViewProvider;

    @Inject
    private CredentialConverter credentialConverter;

    @Inject
    private AwsIdentityMappingService awsIdentityMappingService;

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
                    .withRdsConfigs(rdsConfigs)
                    .withHostgroupViews(hostgroupViews)
                    .withGateway(gateway, gatewaySignKey)
                    .withBlueprintView(blueprintView)
                    .withStackRepoDetailsHdpVersion(blueprintStackInfo.getVersion())
                    .withFileSystemConfigurationView(fileSystemConfigurationView)
                    .withGeneralClusterConfigs(generalClusterConfigs)
                    .withLdapConfig(ldapConfig)
                    .withKerberosConfig(getKerberosConfig(source));

            SharedServiceV4Request sharedService = source.getSharedService();
            if (sharedService != null && StringUtils.isNotBlank(sharedService.getDatalakeName())) {
                DatalakeResources datalakeResource = datalakeResourcesService.getByNameForWorkspace(source.getSharedService().getDatalakeName(), workspace);
                if (datalakeResource != null) {
                    DatalakeConfigApi connector = getDatalakeConnector(datalakeResource);
                    SharedServiceConfigsView sharedServiceConfigsView = ambariDatalakeConfigProvider.createSharedServiceConfigView(datalakeResource);
                    Map<String, String> blueprintConfigParams =
                            ambariDatalakeConfigProvider.getBlueprintConfigParameters(datalakeResource, blueprint, connector);
                    Map<String, String> additionalParams = ambariDatalakeConfigProvider.getAdditionalParameters(source, datalakeResource);
                    builder.withSharedServiceConfigs(sharedServiceConfigsView)
                            .withFixInputs((Map) additionalParams)
                            .withCustomInputs((Map) blueprintConfigParams);
                } else {
                    throw new CloudbreakServiceException("Cannot collect shared service resources from datalake!");
                }
            }
            if (environment.getIdBrokerMappingSource() == IdBrokerMappingSource.MOCK && source.getCloudPlatform() == CloudPlatform.AWS) {
                Map<String, String> groupMapping = awsIdentityMappingService.getIdentityGroupMapping(credential);
                Map<String, String> userMapping = awsIdentityMappingService.getIdentityUserMapping(credential);
                builder.withIdentityGroupMapping(groupMapping);
                builder.withIdentityUserMapping(userMapping);
            }

            return builder.build();
        } catch (BlueprintProcessingException | IOException e) {
            throw new CloudbreakServiceException(e.getMessage(), e);
        }
    }

    public DatalakeConfigApi getDatalakeConnector(DatalakeResources datalakeResources) {
        if (datalakeResources.getDatalakeStackId() != null) {
            Stack datalakeStack = stackService.getById(datalakeResources.getDatalakeStackId());
            return datalakeConfigApiConnector.getConnector(datalakeStack);
        } else {
            throw new CloudbreakServiceException("Can not create Ambari Clientas there is no Datalake Stack and the credential is not for Cumulus");
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
        if (cloudStorageValidationUtil.isCloudStorageConfigured(source.getCluster().getCloudStorage())) {
            FileSystem fileSystem = getConversionService().convert(source.getCluster().getCloudStorage(), FileSystem.class);
            fileSystemConfigurationView = fileSystemConfigurationProvider.fileSystemConfiguration(fileSystem, source, credentialAttributes);
        }
        return fileSystemConfigurationView;
    }

    private LdapView getLdapConfig(StackV4Request source) {
        return ldapConfigService.get(source.getEnvironmentCrn()).orElse(null);
    }

    private KerberosConfig getKerberosConfig(StackV4Request source) {
        return kerberosConfigService.get(source.getEnvironmentCrn()).orElse(null);
    }
}
