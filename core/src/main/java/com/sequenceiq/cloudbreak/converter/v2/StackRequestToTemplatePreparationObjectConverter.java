package com.sequenceiq.cloudbreak.converter.v2;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.google.common.base.Strings;
import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.cloudbreak.api.model.SharedServiceRequest;
import com.sequenceiq.cloudbreak.api.model.v2.GeneralSettings;
import com.sequenceiq.cloudbreak.api.model.v2.InstanceGroupV2Request;
import com.sequenceiq.cloudbreak.api.model.v2.StackV2Request;
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
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.DatalakeResources;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.Gateway;
import com.sequenceiq.cloudbreak.domain.view.EnvironmentView;
import com.sequenceiq.cloudbreak.domain.workspace.User;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;
import com.sequenceiq.cloudbreak.service.CloudbreakRestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.service.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.service.TransactionService;
import com.sequenceiq.cloudbreak.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.cloudbreak.service.TransactionService.TransactionRuntimeExecutionException;
import com.sequenceiq.cloudbreak.service.clusterdefinition.ClusterDefinitionService;
import com.sequenceiq.cloudbreak.service.cluster.ambari.AmbariClientFactory;
import com.sequenceiq.cloudbreak.service.credential.CredentialService;
import com.sequenceiq.cloudbreak.service.environment.EnvironmentViewService;
import com.sequenceiq.cloudbreak.service.flex.FlexSubscriptionService;
import com.sequenceiq.cloudbreak.service.kerberos.KerberosService;
import com.sequenceiq.cloudbreak.service.ldapconfig.LdapConfigService;
import com.sequenceiq.cloudbreak.service.rdsconfig.RdsConfigService;
import com.sequenceiq.cloudbreak.service.sharedservice.DatalakeConfigProvider;
import com.sequenceiq.cloudbreak.service.stack.StackService;
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
public class StackRequestToTemplatePreparationObjectConverter extends AbstractConversionServiceAwareConverter<StackV2Request, TemplatePreparationObject> {

    @Inject
    private FlexSubscriptionService flexSubscriptionService;

    @Inject
    private LdapConfigService ldapConfigService;

    @Inject
    private StackService stackService;

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
    private TransactionService transactionService;

    @Override
    public TemplatePreparationObject convert(StackV2Request source) {
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
            ClusterDefinition clusterDefinition = getBlueprint(source, workspace);
            String clusterDefinitionText = clusterDefinition.getClusterDefinitionText();
            ClusterDefinitionStackInfo clusterDefinitionStackInfo = stackInfoService.blueprintStackInfo(clusterDefinitionText);
            Set<HostgroupView> hostgroupViews = getHostgroupViews(source);
            Gateway gateway = source.getCluster().getAmbari().getGateway() == null ? null : getConversionService().convert(source, Gateway.class);
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
                    .withBlueprintView(clusterDefinitionView)
                    .withStackRepoDetailsHdpVersion(clusterDefinitionStackInfo.getVersion())
                    .withFileSystemConfigurationView(fileSystemConfigurationView)
                    .withGeneralClusterConfigs(generalClusterConfigs)
                    .withSmartSenseSubscription(smartsenseSubscription)
                    .withLdapConfig(ldapConfig, bindDn, bindPassword)
                    .withKerberosConfig(kerberosConfig);

            SharedServiceRequest sharedService = source.getCluster().getSharedService();
            if (sharedService != null && !Strings.isNullOrEmpty(sharedService.getSharedCluster())) {
                try {
                    transactionService.required(() -> {
                        Stack dataLakeStack = stackService.getByNameInWorkspaceWithLists(sharedService.getSharedCluster(), workspace.getId());
                        AmbariClient datalakeAmbariClient = ambariClientFactory.getAmbariClient(dataLakeStack, dataLakeStack.getCluster());
                        DatalakeResources datalakeResources = datalakeConfigProvider.collectAndStoreDatalakeResources(dataLakeStack, datalakeAmbariClient);
                        if (datalakeResources != null) {
                            SharedServiceConfigsView sharedServiceConfigsView = datalakeConfigProvider.createSharedServiceConfigView(datalakeResources);
                            Map<String, String> blueprintConfigParams =
                                    datalakeConfigProvider.getBlueprintConfigParameters(datalakeResources, clusterDefinition, datalakeAmbariClient);
                            Map<String, String> additionalParams = datalakeConfigProvider.getAdditionalParameters(source, datalakeResources);
                            builder.withSharedServiceConfigs(sharedServiceConfigsView)
                                    .withFixInputs((Map) additionalParams)
                                    .withCustomInputs((Map) blueprintConfigParams);
                            return null;
                        } else {
                            throw new CloudbreakServiceException("Cannot collect shared service resources from datalake!");
                        }
                    });
                } catch (TransactionExecutionException e) {
                    throw new TransactionRuntimeExecutionException(e);
                }
            }
            return builder.build();
        } catch (ClusterDefinitionProcessingException | IOException e) {
            throw new CloudbreakServiceException(e.getMessage(), e);
        }
    }

    private Credential getCredential(StackV2Request source, Workspace workspace) {
        GeneralSettings generalSettings = source.getGeneral();
        Credential credential;
        if (!StringUtils.isEmpty(generalSettings.getEnvironmentName())) {
            EnvironmentView environmentView = environmentViewService.getByNameForWorkspace(generalSettings.getEnvironmentName(), workspace);
            credential = environmentView.getCredential();
        } else {
            credential = credentialService.getByNameForWorkspace(source.getGeneral().getCredentialName(), workspace);
        }
        return credential;
    }

    private ClusterDefinition getBlueprint(StackV2Request source, Workspace workspace) {
        ClusterDefinition clusterDefinition;
        clusterDefinition = Strings.isNullOrEmpty(source.getCluster().getAmbari().getBlueprintName())
                ? clusterDefinitionService.get(source.getCluster().getAmbari().getBlueprintId())
                : clusterDefinitionService.getByNameForWorkspace(source.getCluster().getAmbari().getBlueprintName(), workspace);
        return clusterDefinition;
    }

    private Optional<FlexSubscription> getFlexSubscription(StackV2Request source) {
        return source.getFlexId() != null
                ? Optional.ofNullable(flexSubscriptionService.get(source.getFlexId()))
                : Optional.empty();
    }

    private Optional<String> getSmartsenseSubscriptionId(Optional<FlexSubscription> flexSubscription) {
        return flexSubscription.isPresent()
                ? Optional.ofNullable(flexSubscription.get().getSubscriptionId())
                : Optional.empty();
    }

    private Set<HostgroupView> getHostgroupViews(StackV2Request source) {
        Set<HostgroupView> hostgroupViews = new HashSet<>();
        for (InstanceGroupV2Request instanceGroupV2Request : source.getInstanceGroups()) {
            hostgroupViews.add(
                    new HostgroupView(
                            instanceGroupV2Request.getGroup(),
                            instanceGroupV2Request.getTemplate().getVolumeCount(),
                            instanceGroupV2Request.getType(),
                            instanceGroupV2Request.getNodeCount()));
        }
        return hostgroupViews;
    }

    private Set<RDSConfig> getRdsConfigs(StackV2Request source, Workspace workspace) {
        Set<RDSConfig> rdsConfigs = new HashSet<>();
        for (String rdsConfigRequest : source.getCluster().getRdsConfigNames()) {
            RDSConfig rdsConfig = rdsConfigService.getByNameForWorkspace(rdsConfigRequest, workspace);
            rdsConfigs.add(rdsConfig);
        }
        return rdsConfigs;
    }

    private BaseFileSystemConfigurationsView getFileSystemConfigurationView(StackV2Request source, Credential credential) throws IOException {
        BaseFileSystemConfigurationsView fileSystemConfigurationView = null;
        if (cloudStorageValidationUtil.isCloudStorageConfigured(source.getCluster().getCloudStorage())) {
            FileSystem fileSystem = getConversionService().convert(source.getCluster().getCloudStorage(), FileSystem.class);
            fileSystemConfigurationView = fileSystemConfigurationProvider.fileSystemConfiguration(fileSystem, source, credential);
        }
        return fileSystemConfigurationView;
    }

    private LdapConfig getLdapConfig(StackV2Request source, Workspace workspace) {
        LdapConfig ldapConfig = null;
        if (source.getCluster().getLdapConfigName() != null) {
            ldapConfig = ldapConfigService.getByNameForWorkspace(source.getCluster().getLdapConfigName(), workspace);
        }
        return ldapConfig;
    }

    private KerberosConfig getKerberosConfig(StackV2Request source) {
        KerberosConfig kerberosConfig = null;
        if (StringUtils.isNotBlank(source.getCluster().getAmbari().getKerberosConfigName())) {
            kerberosConfig = kerberosService.getByNameForWorkspaceId(source.getCluster().getAmbari().getKerberosConfigName(),
                    restRequestThreadLocalService.getRequestedWorkspaceId());
        }
        return kerberosConfig;
    }
}
