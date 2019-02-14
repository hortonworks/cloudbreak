package com.sequenceiq.cloudbreak.converter.v4.stacks;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.sharedservice.SharedServiceV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.InstanceGroupV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.template.volume.VolumeV4Request;
import com.sequenceiq.cloudbreak.clusterdefinition.GeneralClusterConfigsProvider;
import com.sequenceiq.cloudbreak.clusterdefinition.utils.StackInfoService;
import com.sequenceiq.cloudbreak.common.model.user.CloudbreakUser;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.converter.util.CloudStorageValidationUtil;
import com.sequenceiq.cloudbreak.domain.ClusterDefinition;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.FileSystem;
import com.sequenceiq.cloudbreak.domain.FlexSubscription;
import com.sequenceiq.cloudbreak.domain.KerberosConfig;
import com.sequenceiq.cloudbreak.domain.LdapConfig;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.domain.SmartSenseSubscription;
import com.sequenceiq.cloudbreak.domain.stack.cluster.DatalakeResources;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.Gateway;
import com.sequenceiq.cloudbreak.domain.view.EnvironmentView;
import com.sequenceiq.cloudbreak.domain.workspace.User;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;
import com.sequenceiq.cloudbreak.service.CloudbreakRestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.service.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.service.cluster.ambari.AmbariClientFactory;
import com.sequenceiq.cloudbreak.service.clusterdefinition.ClusterDefinitionService;
import com.sequenceiq.cloudbreak.service.credential.CredentialService;
import com.sequenceiq.cloudbreak.service.datalake.DatalakeResourcesService;
import com.sequenceiq.cloudbreak.service.environment.EnvironmentViewService;
import com.sequenceiq.cloudbreak.service.flex.FlexSubscriptionService;
import com.sequenceiq.cloudbreak.service.kerberos.KerberosService;
import com.sequenceiq.cloudbreak.service.ldapconfig.LdapConfigService;
import com.sequenceiq.cloudbreak.service.rdsconfig.RdsConfigService;
import com.sequenceiq.cloudbreak.service.sharedservice.DatalakeConfigProvider;
import com.sequenceiq.cloudbreak.service.user.UserService;
import com.sequenceiq.cloudbreak.service.workspace.WorkspaceService;
import com.sequenceiq.cloudbreak.template.ClusterDefinitionProcessingException;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject.Builder;
import com.sequenceiq.cloudbreak.template.filesystem.BaseFileSystemConfigurationsView;
import com.sequenceiq.cloudbreak.template.filesystem.FileSystemConfigurationProvider;
import com.sequenceiq.cloudbreak.template.model.ClusterDefinitionStackInfo;
import com.sequenceiq.cloudbreak.template.model.GeneralClusterConfigs;
import com.sequenceiq.cloudbreak.template.views.ClusterDefinitionView;
import com.sequenceiq.cloudbreak.template.views.HostgroupView;
import com.sequenceiq.cloudbreak.template.views.SharedServiceConfigsView;

@Component
public class StackV4RequestToTemplatePreparationObjectConverter extends AbstractConversionServiceAwareConverter<StackV4Request, TemplatePreparationObject> {

    @Inject
    private FlexSubscriptionService flexSubscriptionService;

    @Inject
    private LdapConfigService ldapConfigService;

    @Inject
    private RdsConfigService rdsConfigService;

    @Inject
    private GeneralClusterConfigsProvider generalClusterConfigsProvider;

    @Inject
    private ClusterDefinitionService clusterDefinitionService;

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
    private KerberosService kerberosService;

    @Inject
    private DatalakeConfigProvider datalakeConfigProvider;

    @Inject
    private AmbariClientFactory ambariClientFactory;

    @Inject
    private DatalakeResourcesService datalakeResourcesService;

    @Override
    public TemplatePreparationObject convert(StackV4Request source) {
        try {
            CloudbreakUser cloudbreakUser = restRequestThreadLocalService.getCloudbreakUser();
            User user = userService.getOrCreate(cloudbreakUser);
            Workspace workspace = workspaceService.get(restRequestThreadLocalService.getRequestedWorkspaceId(), user);
            Credential credential = getCredential(source, workspace);
            Optional<FlexSubscription> flexSubscription = getFlexSubscription(source);
            SmartSenseSubscription smartsenseSubscription = flexSubscription.map(FlexSubscription::getSmartSenseSubscription).orElse(null);
            KerberosConfig kerberosConfig = getKerberosConfig(source);
            LdapConfig ldapConfig = getLdapConfig(source, workspace);
            BaseFileSystemConfigurationsView fileSystemConfigurationView = getFileSystemConfigurationView(source, credential);
            Set<RDSConfig> rdsConfigs = getRdsConfigs(source, workspace);
            ClusterDefinition clusterDefinition = getClusterDefinition(source, workspace);
            String clusterDefinitionText = clusterDefinition.getClusterDefinitionText();
            ClusterDefinitionStackInfo clusterDefinitionStackInfo = stackInfoService.blueprintStackInfo(clusterDefinitionText);
            Set<HostgroupView> hostgroupViews = getHostgroupViews(source);
            Gateway gateway = source.getCluster().getGateway() == null ? null : getConversionService().convert(source, Gateway.class);
            ClusterDefinitionView clusterDefinitionView = new ClusterDefinitionView(clusterDefinition.getClusterDefinitionText(),
                    clusterDefinitionStackInfo.getVersion(), clusterDefinitionStackInfo.getType());
            GeneralClusterConfigs generalClusterConfigs = generalClusterConfigsProvider.generalClusterConfigs(source, user, cloudbreakUser.getEmail());
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
                    .withFlexSubscription(flexSubscription.orElse(null))
                    .withRdsConfigs(rdsConfigs)
                    .withHostgroupViews(hostgroupViews)
                    .withGateway(gateway, gatewaySignKey)
                    .withClusterDefinitionView(clusterDefinitionView)
                    .withStackRepoDetailsHdpVersion(clusterDefinitionStackInfo.getVersion())
                    .withFileSystemConfigurationView(fileSystemConfigurationView)
                    .withGeneralClusterConfigs(generalClusterConfigs)
                    .withSmartSenseSubscription(smartsenseSubscription)
                    .withLdapConfig(ldapConfig, bindDn, bindPassword)
                    .withKerberosConfig(kerberosConfig);

            SharedServiceV4Request sharedService = source.getSharedService();
            if (sharedService != null && StringUtils.isNotBlank(sharedService.getDatalakeName())) {
                DatalakeResources datalakeResource = datalakeResourcesService.getByNameForWorkspace(source.getSharedService().getDatalakeName(), workspace);
                if (datalakeResource != null) {
                    AmbariClient datalakeAmbariClient = ambariClientFactory.getAmbariClient(datalakeResource, credential);
                    SharedServiceConfigsView sharedServiceConfigsView = datalakeConfigProvider.createSharedServiceConfigView(datalakeResource);
                    Map<String, String> blueprintConfigParams =
                            datalakeConfigProvider.getBlueprintConfigParameters(datalakeResource, clusterDefinition, datalakeAmbariClient);
                    Map<String, String> additionalParams = datalakeConfigProvider.getAdditionalParameters(source, datalakeResource);
                    builder.withSharedServiceConfigs(sharedServiceConfigsView)
                            .withFixInputs((Map) additionalParams)
                            .withCustomInputs((Map) blueprintConfigParams);
                } else {
                    throw new CloudbreakServiceException("Cannot collect shared service resources from datalake!");
                }
            }
            return builder.build();
        } catch (ClusterDefinitionProcessingException | IOException e) {
            throw new CloudbreakServiceException(e.getMessage(), e);
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

    private ClusterDefinition getClusterDefinition(StackV4Request source, Workspace workspace) {
        return clusterDefinitionService.getByNameForWorkspace(source.getCluster().getAmbari().getBlueprintName(), workspace);
    }

    private Optional<FlexSubscription> getFlexSubscription(StackV4Request source) {
        return source.getFlexId() != null
                ? Optional.ofNullable(flexSubscriptionService.get(source.getFlexId()))
                : Optional.empty();
    }

    private Optional<String> getSmartsenseSubscriptionId(Optional<FlexSubscription> flexSubscription) {
        return flexSubscription.isPresent()
                ? Optional.ofNullable(flexSubscription.get().getSubscriptionId())
                : Optional.empty();
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
            kerberosConfig = kerberosService.getByNameForWorkspaceId(source.getCluster().getKerberosName(),
                    restRequestThreadLocalService.getRequestedWorkspaceId());
        }
        return kerberosConfig;
    }
}
