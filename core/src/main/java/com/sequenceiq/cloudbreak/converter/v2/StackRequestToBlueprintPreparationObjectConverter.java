package com.sequenceiq.cloudbreak.converter.v2;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.api.model.ConfigsResponse;
import com.sequenceiq.cloudbreak.api.model.FileSystemConfiguration;
import com.sequenceiq.cloudbreak.api.model.SharedServiceRequest;
import com.sequenceiq.cloudbreak.api.model.v2.InstanceGroupV2Request;
import com.sequenceiq.cloudbreak.api.model.v2.StackV2Request;
import com.sequenceiq.cloudbreak.blueprint.BlueprintPreparationObject;
import com.sequenceiq.cloudbreak.blueprint.BlueprintProcessingException;
import com.sequenceiq.cloudbreak.blueprint.GeneralClusterConfigsProvider;
import com.sequenceiq.cloudbreak.blueprint.filesystem.FileSystemConfigurationProvider;
import com.sequenceiq.cloudbreak.blueprint.sharedservice.SharedServiceConfigsViewProvider;
import com.sequenceiq.cloudbreak.blueprint.template.views.BlueprintView;
import com.sequenceiq.cloudbreak.blueprint.template.views.FileSystemConfigurationView;
import com.sequenceiq.cloudbreak.blueprint.template.views.HostgroupView;
import com.sequenceiq.cloudbreak.blueprint.template.views.SharedServiceConfigsView;
import com.sequenceiq.cloudbreak.blueprint.templates.BlueprintStackInfo;
import com.sequenceiq.cloudbreak.blueprint.templates.GeneralClusterConfigs;
import com.sequenceiq.cloudbreak.blueprint.utils.StackInfoService;
import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;
import com.sequenceiq.cloudbreak.common.service.user.UserFilterField;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.FileSystem;
import com.sequenceiq.cloudbreak.domain.FlexSubscription;
import com.sequenceiq.cloudbreak.domain.KerberosConfig;
import com.sequenceiq.cloudbreak.domain.LdapConfig;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.Gateway;
import com.sequenceiq.cloudbreak.service.AuthenticatedUserService;
import com.sequenceiq.cloudbreak.service.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintService;
import com.sequenceiq.cloudbreak.service.filesystem.FileSystemConfigService;
import com.sequenceiq.cloudbreak.service.flex.FlexSubscriptionService;
import com.sequenceiq.cloudbreak.service.ldapconfig.LdapConfigService;
import com.sequenceiq.cloudbreak.service.rdsconfig.RdsConfigService;
import com.sequenceiq.cloudbreak.service.sharedservice.SharedServiceConfigProvider;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.user.UserDetailsService;

@Component
public class StackRequestToBlueprintPreparationObjectConverter extends AbstractConversionServiceAwareConverter<StackV2Request, BlueprintPreparationObject> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackRequestToBlueprintPreparationObjectConverter.class);

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
    private UserDetailsService userDetailsService;

    @Inject
    private BlueprintService blueprintService;

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
    private AuthenticatedUserService authenticatedUserService;

    @Override
    public BlueprintPreparationObject convert(StackV2Request source) {
        try {
            IdentityUser identityUser = userDetailsService.getDetails(source.getOwner(), UserFilterField.USERID);
            FlexSubscription flexSubscription = getFlexSubscription(source);
            String smartsenseSubscriptionId = getSmartsenseSubscriptionId(source, flexSubscription);
            KerberosConfig kerberosConfig = getKerberosConfig(source);
            LdapConfig ldapConfig = getLdapConfig(source, identityUser);
            FileSystemConfigurationView fileSystemConfigurationView = getFileSystemConfigurationView(source);
            Set<RDSConfig> rdsConfigs = getRdsConfigs(source, identityUser);
            Blueprint blueprint = getBlueprint(source, identityUser);
            BlueprintStackInfo blueprintStackInfo = stackInfoService.blueprintStackInfo(blueprint.getBlueprintText());
            Set<HostgroupView> hostgroupViews = getHostgroupViews(source);
            Gateway gateway = getConversionService().convert(source, Gateway.class);
            BlueprintView blueprintView = new BlueprintView(blueprint.getBlueprintText(), blueprintStackInfo.getVersion(), blueprintStackInfo.getType());
            GeneralClusterConfigs generalClusterConfigs = generalClusterConfigsProvider.generalClusterConfigs(source, identityUser);
            BlueprintPreparationObject.Builder builder = BlueprintPreparationObject.Builder.builder()
                    .withFlexSubscription(flexSubscription)
                    .withRdsConfigs(rdsConfigs)
                    .withHostgroupViews(hostgroupViews)
                    .withGateway(gateway)
                    .withBlueprintView(blueprintView)
                    .withStackRepoDetailsHdpVersion(blueprintStackInfo.getVersion())
                    .withFileSystemConfigurationView(fileSystemConfigurationView)
                    .withGeneralClusterConfigs(generalClusterConfigs)
                    .withSmartSenseSubscriptionId(smartsenseSubscriptionId)
                    .withLdapConfig(ldapConfig)
                    .withKerberosConfig(kerberosConfig);

            SharedServiceRequest sharedService = source.getCluster().getSharedService();
            if (sharedService != null && !Strings.isNullOrEmpty(sharedService.getSharedCluster())) {
                Stack dataLakeStack = stackService.getPublicStack(sharedService.getSharedCluster(), identityUser);
                SharedServiceConfigsView sharedServiceConfigsView = sharedServiceConfigsViewProvider
                        .createSharedServiceConfigs(blueprint, source.getCluster().getAmbari().getPassword(), dataLakeStack);
                ConfigsResponse configsResponse = sharedServiceConfigProvider.retrieveOutputs(dataLakeStack, blueprint, source.getGeneral().getName());
                builder.withSharedServiceConfigs(sharedServiceConfigsView)
                        .withFixInputs(configsResponse.getFixInputs())
                        .withCustomInputs(configsResponse.getDatalakeInputs());

            }
            return builder.build();
        } catch (BlueprintProcessingException e) {
            throw new CloudbreakServiceException(e.getMessage(), e);
        } catch (IOException e) {
            throw new CloudbreakServiceException(e.getMessage(), e);
        }
    }

    private Blueprint getBlueprint(StackV2Request source, IdentityUser identityUser) {
        Blueprint blueprint;
        if (Strings.isNullOrEmpty(source.getCluster().getAmbari().getBlueprintName())) {
            blueprint = blueprintService.get(source.getCluster().getAmbari().getBlueprintId());
        } else {
            blueprint = blueprintService.get(source.getCluster().getAmbari().getBlueprintName(), identityUser.getAccount());
        }
        return blueprint;
    }

    private FlexSubscription getFlexSubscription(StackV2Request source) {
        FlexSubscription flexSubscription = null;
        if (source.getFlexId() != null) {
            flexSubscription = flexSubscriptionService.findOneById(source.getFlexId());
        }
        return flexSubscription;
    }

    private String getSmartsenseSubscriptionId(StackV2Request source, FlexSubscription flexSubscription) {
        String smartsenseSubscriptionId = null;
        if (source.getFlexId() != null) {
            smartsenseSubscriptionId = flexSubscription.getSubscriptionId();
        }
        return smartsenseSubscriptionId;
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

    private Set<RDSConfig> getRdsConfigs(StackV2Request source, IdentityUser identityUser) {
        Set<RDSConfig> rdsConfigs = new HashSet<>();
        for (String rdsConfigRequest : source.getCluster().getRdsConfigNames()) {
            RDSConfig rdsConfig = rdsConfigService.getPrivateRdsConfig(rdsConfigRequest, identityUser);
            rdsConfigs.add(rdsConfig);
        }
        return rdsConfigs;
    }

    private FileSystemConfigurationView getFileSystemConfigurationView(StackV2Request source) throws IOException {
        FileSystemConfigurationView fileSystemConfigurationView = null;
        if (source.getCluster().getFileSystem() != null) {
            FileSystem fileSystem = getConversionService().convert(source.getCluster().getFileSystem(), FileSystem.class);

            FileSystemConfiguration fileSystemConfiguration = fileSystemConfigurationProvider.fileSystemConfiguration(fileSystem, null);
            fileSystemConfigurationView = new FileSystemConfigurationView(fileSystemConfiguration, fileSystem.isDefaultFs());
        }
        return fileSystemConfigurationView;
    }

    private LdapConfig getLdapConfig(StackV2Request source, IdentityUser identityUser) {
        LdapConfig ldapConfig = null;
        if (source.getCluster().getLdapConfigName() != null) {
            ldapConfig = ldapConfigService.getPublicConfig(source.getCluster().getLdapConfigName(), identityUser);
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
