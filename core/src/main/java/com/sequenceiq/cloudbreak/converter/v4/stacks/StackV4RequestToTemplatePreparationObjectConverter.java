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
import com.sequenceiq.cloudbreak.cluster.api.DatalakeConfigApi;
import com.sequenceiq.cloudbreak.blueprint.GeneralClusterConfigsProvider;
import com.sequenceiq.cloudbreak.blueprint.utils.StackInfoService;
import com.sequenceiq.cloudbreak.common.model.user.CloudbreakUser;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.converter.util.CloudStorageValidationUtil;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.FileSystem;
import com.sequenceiq.cloudbreak.domain.KerberosConfig;
import com.sequenceiq.cloudbreak.domain.LdapConfig;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.DatalakeResources;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.Gateway;
import com.sequenceiq.cloudbreak.domain.view.EnvironmentView;
import com.sequenceiq.cloudbreak.domain.workspace.User;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;
import com.sequenceiq.cloudbreak.service.CloudbreakRestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.service.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintService;
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintTextProcessorFactory;
import com.sequenceiq.cloudbreak.service.credential.CredentialPrerequisiteService;
import com.sequenceiq.cloudbreak.service.credential.CredentialService;
import com.sequenceiq.cloudbreak.service.datalake.DatalakeResourcesService;
import com.sequenceiq.cloudbreak.service.environment.EnvironmentViewService;
import com.sequenceiq.cloudbreak.service.kerberos.KerberosConfigService;
import com.sequenceiq.cloudbreak.service.ldapconfig.LdapConfigService;
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
    private CredentialService credentialService;

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
    private EnvironmentViewService environmentViewService;

    @Inject
    private KerberosConfigService kerberosConfigService;

    @Inject
    private AmbariDatalakeConfigProvider ambariDatalakeConfigProvider;

    @Inject
    private DatalakeResourcesService datalakeResourcesService;

    @Inject
    private CredentialPrerequisiteService credentialPrerequisiteService;

    @Inject
    private StackService stackService;

    @Inject
    private DatalakeConfigApiConnector datalakeConfigApiConnector;

    @Inject
    private BlueprintTextProcessorFactory blueprintTextProcessorFactory;

    @Override
    public TemplatePreparationObject convert(StackV4Request source) {
        try {
            CloudbreakUser cloudbreakUser = restRequestThreadLocalService.getCloudbreakUser();
            User user = userService.getOrCreate(cloudbreakUser);
            Workspace workspace = workspaceService.get(restRequestThreadLocalService.getRequestedWorkspaceId(), user);
            Credential credential = getCredential(source, workspace);
            KerberosConfig kerberosConfig = getKerberosConfig(source);
            LdapConfig ldapConfig = getLdapConfig(source, workspace);
            BaseFileSystemConfigurationsView fileSystemConfigurationView = getFileSystemConfigurationView(source, credential);
            Set<RDSConfig> rdsConfigs = getRdsConfigs(source, workspace);
            Blueprint blueprint = getBlueprint(source, workspace);
            String blueprintText = blueprint.getBlueprintText();
            BlueprintStackInfo blueprintStackInfo = stackInfoService.blueprintStackInfo(blueprintText);
            Set<HostgroupView> hostgroupViews = getHostgroupViews(source);
            Gateway gateway = source.getCluster().getGateway() == null || blueprintService.isClouderaManagerTemplate(blueprint)
                    ? null : getConversionService().convert(source, Gateway.class);
            BlueprintView blueprintView = new BlueprintView(blueprint.getBlueprintText(),
                    blueprintStackInfo.getVersion(), blueprintStackInfo.getType(),
                    blueprintTextProcessorFactory.createBlueprintTextProcessor(blueprint.getBlueprintText()));
            GeneralClusterConfigs generalClusterConfigs = generalClusterConfigsProvider.generalClusterConfigs(source, cloudbreakUser.getEmail(),
                    blueprintService.getBlueprintVariant(blueprint));
            String bindDn = null;
            String bindPassword = null;
            if (ldapConfig != null) {
                bindDn = ldapConfig.getBindDn();
                bindPassword = ldapConfig.getBindPassword();
            }
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
                    .withLdapConfig(ldapConfig, bindDn, bindPassword)
                    .withKerberosConfig(kerberosConfig);

            SharedServiceV4Request sharedService = source.getSharedService();
            if (sharedService != null && StringUtils.isNotBlank(sharedService.getDatalakeName())) {
                DatalakeResources datalakeResource = datalakeResourcesService.getByNameForWorkspace(source.getSharedService().getDatalakeName(), workspace);
                if (datalakeResource != null) {
                    DatalakeConfigApi connector = getDatalakeConnector(datalakeResource, credential);
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
            return builder.build();
        } catch (BlueprintProcessingException | IOException e) {
            throw new CloudbreakServiceException(e.getMessage(), e);
        }
    }

    public DatalakeConfigApi getDatalakeConnector(DatalakeResources datalakeResources, Credential credential) {
        if (datalakeResources.getDatalakeStackId() != null) {
            Stack datalakeStack = stackService.getById(datalakeResources.getDatalakeStackId());
            return datalakeConfigApiConnector.getConnector(datalakeStack);
        } else if (credentialPrerequisiteService.isCumulusCredential(credential.getAttributes())) {
            return credentialPrerequisiteService.createCumulusDatalakeConnector(credential.getAttributes());
        } else {
            throw new CloudbreakServiceException("Can not create Ambari Clientas there is no Datalake Stack and the credential is not for Cumulus");
        }
    }

    private Credential getCredential(StackV4Request source, Workspace workspace) {
        String environmentName = source.getEnvironment().getName();
        if (!StringUtils.isEmpty(environmentName)) {
            EnvironmentView environmentView = environmentViewService.getByNameForWorkspace(environmentName, workspace);
            return environmentView.getCredential();
        }
        return credentialService.getByNameForWorkspace(source.getEnvironment().getCredentialName(), workspace);
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

    private BaseFileSystemConfigurationsView getFileSystemConfigurationView(StackV4Request source, Credential credential) throws IOException {
        BaseFileSystemConfigurationsView fileSystemConfigurationView = null;
        if (cloudStorageValidationUtil.isCloudStorageConfigured(source.getCluster().getCloudStorage())) {
            FileSystem fileSystem = getConversionService().convert(source.getCluster().getCloudStorage(), FileSystem.class);
            fileSystemConfigurationView = fileSystemConfigurationProvider.fileSystemConfiguration(fileSystem, source, credential);
        }
        return fileSystemConfigurationView;
    }

    private LdapConfig getLdapConfig(StackV4Request source, Workspace workspace) {
        LdapConfig ldapConfig = null;
        if (source.getCluster().getLdapName() != null) {
            ldapConfig = ldapConfigService.getByNameForWorkspace(source.getCluster().getLdapName(), workspace);
        }
        return ldapConfig;
    }

    private KerberosConfig getKerberosConfig(StackV4Request source) {
        KerberosConfig kerberosConfig = null;
        if (StringUtils.isNotBlank(source.getCluster().getKerberosName())) {
            kerberosConfig = kerberosConfigService.getByNameForWorkspaceId(source.getCluster().getKerberosName(),
                    restRequestThreadLocalService.getRequestedWorkspaceId());
        }
        return kerberosConfig;
    }
}
