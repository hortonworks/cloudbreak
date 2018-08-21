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
import com.sequenceiq.cloudbreak.blueprint.BlueprintPreparationObject;
import com.sequenceiq.cloudbreak.blueprint.BlueprintPreparationObject.Builder;
import com.sequenceiq.cloudbreak.blueprint.BlueprintProcessingException;
import com.sequenceiq.cloudbreak.blueprint.GeneralClusterConfigsProvider;
import com.sequenceiq.cloudbreak.blueprint.filesystem.BaseFileSystemConfigurationsView;
import com.sequenceiq.cloudbreak.blueprint.filesystem.FileSystemConfigurationProvider;
import com.sequenceiq.cloudbreak.blueprint.filesystem.FileSystemConfigurationsViewProvider;
import com.sequenceiq.cloudbreak.blueprint.sharedservice.SharedServiceConfigsViewProvider;
import com.sequenceiq.cloudbreak.blueprint.template.views.BlueprintView;
import com.sequenceiq.cloudbreak.blueprint.template.views.HostgroupView;
import com.sequenceiq.cloudbreak.blueprint.template.views.SharedServiceConfigsView;
import com.sequenceiq.cloudbreak.blueprint.templates.BlueprintStackInfo;
import com.sequenceiq.cloudbreak.blueprint.templates.GeneralClusterConfigs;
import com.sequenceiq.cloudbreak.blueprint.utils.StackInfoService;
import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;
import com.sequenceiq.cloudbreak.common.service.user.UserFilterField;
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
import com.sequenceiq.cloudbreak.domain.organization.Organization;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.Gateway;
import com.sequenceiq.cloudbreak.service.AuthenticatedUserService;
import com.sequenceiq.cloudbreak.service.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintService;
import com.sequenceiq.cloudbreak.service.credential.CredentialService;
import com.sequenceiq.cloudbreak.service.flex.FlexSubscriptionService;
import com.sequenceiq.cloudbreak.service.ldapconfig.LegacyLdapConfigService;
import com.sequenceiq.cloudbreak.service.organization.OrganizationService;
import com.sequenceiq.cloudbreak.service.rdsconfig.LegacyRdsConfigService;
import com.sequenceiq.cloudbreak.service.sharedservice.SharedServiceConfigProvider;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.user.CachedUserDetailsService;

@Component
public class StackRequestToBlueprintPreparationObjectConverter extends AbstractConversionServiceAwareConverter<StackV2Request, BlueprintPreparationObject> {

    @Inject
    private FlexSubscriptionService flexSubscriptionService;

    @Inject
    private LegacyLdapConfigService ldapConfigService;

    @Inject
    private StackService stackService;

    @Inject
    private LegacyRdsConfigService rdsConfigService;

    @Inject
    private GeneralClusterConfigsProvider generalClusterConfigsProvider;

    @Inject
    private CachedUserDetailsService cachedUserDetailsService;

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
    private AuthenticatedUserService authenticatedUserService;

    @Inject
    private FileSystemConfigurationsViewProvider fileSystemConfigurationsViewProvider;

    @Inject
    private CloudStorageValidationUtil cloudStorageValidationUtil;

    @Inject
    private OrganizationService organizationService;

    @Override
    public BlueprintPreparationObject convert(StackV2Request source) {
        try {
            IdentityUser identityUser = cachedUserDetailsService.getDetails(source.getOwner(), UserFilterField.USERID);
            Organization organization = organizationService.getDefaultOrganizationForCurrentUser();
            Credential credential = credentialService.getByNameForOrganization(source.getGeneral().getCredentialName(), organization);
            Optional<FlexSubscription> flexSubscription = getFlexSubscription(source);
            SmartSenseSubscription smartsenseSubscription = flexSubscription.isPresent() ? flexSubscription.get().getSmartSenseSubscription() : null;
            KerberosConfig kerberosConfig = getKerberosConfig(source);
            LdapConfig ldapConfig = getLdapConfig(source, identityUser);
            BaseFileSystemConfigurationsView fileSystemConfigurationView = getFileSystemConfigurationView(source, credential);
            Set<RDSConfig> rdsConfigs = getRdsConfigs(source);
            Blueprint blueprint = getBlueprint(source, organization);
            BlueprintStackInfo blueprintStackInfo = stackInfoService.blueprintStackInfo(blueprint.getBlueprintText());
            Set<HostgroupView> hostgroupViews = getHostgroupViews(source);
            Gateway gateway = source.getCluster().getAmbari().getGateway() == null ? null : getConversionService().convert(source, Gateway.class);
            BlueprintView blueprintView = new BlueprintView(blueprint.getBlueprintText(), blueprintStackInfo.getVersion(), blueprintStackInfo.getType());
            GeneralClusterConfigs generalClusterConfigs = generalClusterConfigsProvider.generalClusterConfigs(source, identityUser);
            Builder builder = Builder.builder()
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
                Stack dataLakeStack = stackService.getByNameInDefaultOrg(sharedService.getSharedCluster());
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

    private Blueprint getBlueprint(StackV2Request source, Organization organization) {
        Blueprint blueprint;
        blueprint = Strings.isNullOrEmpty(source.getCluster().getAmbari().getBlueprintName())
                ? blueprintService.getByIdFromAnyAvailableOrganization(source.getCluster().getAmbari().getBlueprintId())
                : blueprintService.getByNameForOrganization(source.getCluster().getAmbari().getBlueprintName(), organization);
        return blueprint;
    }

    private Optional<FlexSubscription> getFlexSubscription(StackV2Request source) {
        return source.getFlexId() != null
                ? Optional.ofNullable(flexSubscriptionService.getByIdFromAnyAvailableOrganization(source.getFlexId()))
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

    private Set<RDSConfig> getRdsConfigs(StackV2Request source) {
        Set<RDSConfig> rdsConfigs = new HashSet<>();
        for (String rdsConfigRequest : source.getCluster().getRdsConfigNames()) {
            RDSConfig rdsConfig = rdsConfigService.getByNameFromUsersDefaultOrganization(rdsConfigRequest);
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

    private LdapConfig getLdapConfig(StackV2Request source, IdentityUser identityUser) {
        LdapConfig ldapConfig = null;
        if (source.getCluster().getLdapConfigName() != null) {
            ldapConfig = ldapConfigService.getByNameFromUsersDefaultOrganization(source.getCluster().getLdapConfigName());
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
