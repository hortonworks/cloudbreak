package com.sequenceiq.cloudbreak.converter.v2;

import java.io.IOException;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.api.model.ConfigsResponse;
import com.sequenceiq.cloudbreak.api.model.SharedServiceRequest;
import com.sequenceiq.cloudbreak.api.model.v2.InstanceGroupV2Request;
import com.sequenceiq.cloudbreak.api.model.v2.StackV2Request;
import com.sequenceiq.cloudbreak.blueprint.GeneralClusterConfigsProvider;
import com.sequenceiq.cloudbreak.blueprint.sharedservice.SharedServiceConfigsViewProvider;
import com.sequenceiq.cloudbreak.blueprint.utils.StackInfoService;
import com.sequenceiq.cloudbreak.common.model.user.CloudbreakUser;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.converter.util.CloudStorageValidationUtil;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.FileSystem;
import com.sequenceiq.cloudbreak.domain.FlexSubscription;
import com.sequenceiq.cloudbreak.domain.KerberosConfig;
import com.sequenceiq.cloudbreak.domain.LdapConfig;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.domain.SmartSenseSubscription;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.Gateway;
import com.sequenceiq.cloudbreak.domain.workspace.User;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;
import com.sequenceiq.cloudbreak.service.CloudbreakRestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.service.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintService;
import com.sequenceiq.cloudbreak.service.credential.CredentialService;
import com.sequenceiq.cloudbreak.service.filesystem.FileSystemConfigService;
import com.sequenceiq.cloudbreak.service.flex.FlexSubscriptionService;
import com.sequenceiq.cloudbreak.service.ldapconfig.LdapConfigService;
import com.sequenceiq.cloudbreak.service.rdsconfig.RdsConfigService;
import com.sequenceiq.cloudbreak.service.sharedservice.SharedServiceConfigProvider;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.user.UserService;
import com.sequenceiq.cloudbreak.service.workspace.WorkspaceService;
import com.sequenceiq.cloudbreak.template.BlueprintProcessingException;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.filesystem.BaseFileSystemConfigurationsView;
import com.sequenceiq.cloudbreak.template.filesystem.FileSystemConfigurationProvider;
import com.sequenceiq.cloudbreak.template.filesystem.FileSystemConfigurationsViewProvider;
import com.sequenceiq.cloudbreak.template.model.BlueprintStackInfo;
import com.sequenceiq.cloudbreak.template.model.GeneralClusterConfigs;
import com.sequenceiq.cloudbreak.template.views.BlueprintView;
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
    private BlueprintService blueprintService;

    @Inject
    private CredentialService credentialService;

    @Inject
    private FileSystemConfigurationProvider fileSystemConfigurationProvider;

    @Inject
    private StackInfoService stackInfoService;

    @Inject
    private SharedServiceConfigsViewProvider sharedServiceConfigsViewProvider;

    @Inject
    private SharedServiceConfigProvider sharedServiceConfigProvider;

    @Inject
    private FileSystemConfigService fileSystemConfigService;

    @Inject
    private FileSystemConfigurationsViewProvider fileSystemConfigurationsViewProvider;

    @Inject
    private CloudStorageValidationUtil cloudStorageValidationUtil;

    @Inject
    private UserService userService;

    @Inject
    private CloudbreakRestRequestThreadLocalService restRequestThreadLocalService;

    @Inject
    private WorkspaceService workspaceService;

    @Override
    public TemplatePreparationObject convert(StackV2Request source) {
        try {
            CloudbreakUser cloudbreakUser = restRequestThreadLocalService.getCloudbreakUser();
            User user = userService.getOrCreate(cloudbreakUser);
            Workspace workspace = workspaceService.get(restRequestThreadLocalService.getRequestedWorkspaceId(), user);
            Credential credential = credentialService.getByNameForWorkspace(source.getGeneral().getCredentialName(), workspace);
            Optional<FlexSubscription> flexSubscription = getFlexSubscription(source);
            SmartSenseSubscription smartsenseSubscription = flexSubscription.isPresent() ? flexSubscription.get().getSmartSenseSubscription() : null;
            KerberosConfig kerberosConfig = getKerberosConfig(source);
            LdapConfig ldapConfig = getLdapConfig(source, workspace);
            BaseFileSystemConfigurationsView fileSystemConfigurationView = getFileSystemConfigurationView(source, credential);
            Set<RDSConfig> rdsConfigs = getRdsConfigs(source, workspace);
            Blueprint blueprint = getBlueprint(source, workspace);
            BlueprintStackInfo blueprintStackInfo = stackInfoService.blueprintStackInfo(blueprint.getBlueprintText());
            Set<HostgroupView> hostgroupViews = getHostgroupViews(source);
            Gateway gateway = source.getCluster().getAmbari().getGateway() == null ? null : getConversionService().convert(source, Gateway.class);
            BlueprintView blueprintView = new BlueprintView(blueprint.getBlueprintText(), blueprintStackInfo.getVersion(), blueprintStackInfo.getType());
            GeneralClusterConfigs generalClusterConfigs = generalClusterConfigsProvider.generalClusterConfigs(source, user, cloudbreakUser.getEmail());
            TemplatePreparationObject.Builder builder = TemplatePreparationObject.Builder.builder()
                    .withFlexSubscription(flexSubscription.orElse(null))
                    .withRdsConfigs(rdsConfigs)
                    .withHostgroupViews(hostgroupViews)
                    .withGateway(gateway)
                    .withBlueprintView(blueprintView)
                    .withStackRepoDetailsHdpVersion(blueprintStackInfo.getVersion())
                    .withFileSystemConfigurationView(fileSystemConfigurationView)
                    .withGeneralClusterConfigs(generalClusterConfigs)
                    .withSmartSenseSubscription(smartsenseSubscription)
                    .withLdapConfig(ldapConfig)
                    .withKerberosConfig(kerberosConfig);

            SharedServiceRequest sharedService = source.getCluster().getSharedService();
            if (sharedService != null && !Strings.isNullOrEmpty(sharedService.getSharedCluster())) {
                Stack dataLakeStack = stackService.getByNameInWorkspace(sharedService.getSharedCluster(), workspace.getId());
                SharedServiceConfigsView sharedServiceConfigsView = sharedServiceConfigsViewProvider
                        .createSharedServiceConfigs(blueprint, source.getCluster().getAmbari().getPassword(), dataLakeStack);
                ConfigsResponse configsResponse = sharedServiceConfigProvider.retrieveOutputs(dataLakeStack, blueprint, source.getGeneral().getName());
                builder.withSharedServiceConfigs(sharedServiceConfigsView)
                        .withFixInputs(configsResponse.getFixInputs())
                        .withCustomInputs(configsResponse.getDatalakeInputs());

            }
            return builder.build();
        } catch (BlueprintProcessingException | IOException e) {
            throw new CloudbreakServiceException(e.getMessage(), e);
        }
    }

    private Blueprint getBlueprint(StackV2Request source, Workspace workspace) {
        Blueprint blueprint;
        blueprint = Strings.isNullOrEmpty(source.getCluster().getAmbari().getBlueprintName())
                ? blueprintService.get(source.getCluster().getAmbari().getBlueprintId())
                : blueprintService.getByNameForWorkspace(source.getCluster().getAmbari().getBlueprintName(), workspace);
        return blueprint;
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
        if (source.getCluster().getAmbari().getKerberos() != null && source.getCluster().getAmbari().getEnableSecurity()) {
            kerberosConfig = getConversionService().convert(source.getCluster().getAmbari().getKerberos(), KerberosConfig.class);
        }
        return kerberosConfig;
    }
}
