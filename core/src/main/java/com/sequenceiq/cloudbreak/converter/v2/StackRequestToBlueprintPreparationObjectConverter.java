package com.sequenceiq.cloudbreak.converter.v2;

import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.api.model.FileSystemConfiguration;
import com.sequenceiq.cloudbreak.api.model.v2.InstanceGroupV2Request;
import com.sequenceiq.cloudbreak.api.model.v2.StackV2Request;
import com.sequenceiq.cloudbreak.blueprint.GeneralClusterConfigsProvider;
import com.sequenceiq.cloudbreak.blueprint.filesystem.FileSystemConfigurationProvider;
import com.sequenceiq.cloudbreak.blueprint.utils.StackInfoService;
import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;
import com.sequenceiq.cloudbreak.common.service.user.UserFilterField;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.*;
import com.sequenceiq.cloudbreak.service.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintService;
import com.sequenceiq.cloudbreak.service.flex.FlexSubscriptionService;
import com.sequenceiq.cloudbreak.service.ldapconfig.LdapConfigService;
import com.sequenceiq.cloudbreak.service.rdsconfig.RdsConfigService;
import com.sequenceiq.cloudbreak.service.user.UserDetailsService;
import com.sequenceiq.cloudbreak.templateprocessor.processor.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.templateprocessor.processor.TemplateProcessingException;
import com.sequenceiq.cloudbreak.templateprocessor.template.views.BlueprintView;
import com.sequenceiq.cloudbreak.templateprocessor.template.views.FileSystemConfigurationView;
import com.sequenceiq.cloudbreak.templateprocessor.template.views.HostgroupView;
import com.sequenceiq.cloudbreak.templateprocessor.templates.GeneralClusterConfigs;
import com.sequenceiq.cloudbreak.templateprocessor.templates.StackInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

@Component
public class StackRequestToBlueprintPreparationObjectConverter extends AbstractConversionServiceAwareConverter<StackV2Request, TemplatePreparationObject> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackRequestToBlueprintPreparationObjectConverter.class);

    @Inject
    private FlexSubscriptionService flexSubscriptionService;

    @Inject
    private LdapConfigService ldapConfigService;

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

    @Override
    public TemplatePreparationObject convert(StackV2Request source) {
        try {
            IdentityUser identityUser = userDetailsService.getDetails(source.getOwner(), UserFilterField.USERID);
            FlexSubscription flexSubscription = getFlexSubscription(source);
            String smartsenseSubscriptionId = getSmartsenseSubscriptionId(source, flexSubscription);
            KerberosConfig kerberosConfig = getKerberosConfig(source);
            LdapConfig ldapConfig = getLdapConfig(source, identityUser);
            FileSystemConfigurationView fileSystemConfigurationView = getFileSystemConfigurationView(source);
            Set<RDSConfig> rdsConfigs = getRdsConfigs(source, identityUser);
            Blueprint blueprint = getBlueprint(source, identityUser);
            StackInfo blueprintStackInfo = stackInfoService.blueprintStackInfo(blueprint.getBlueprintText());
            Set<HostgroupView> hostgroupViews = getHostgroupViews(source);
            BlueprintView blueprintView = new BlueprintView(blueprint.getBlueprintText(), blueprintStackInfo.getVersion(), blueprintStackInfo.getType());
            GeneralClusterConfigs generalClusterConfigs = generalClusterConfigsProvider.generalClusterConfigs(source, identityUser);

            return TemplatePreparationObject.Builder.builder()
                    .withFlexSubscription(flexSubscription)
                    .withRdsConfigs(rdsConfigs)
                    .withHostgroupViews(hostgroupViews)
                    .withBlueprintView(blueprintView)
                    .withStackRepoDetailsHdpVersion(blueprintStackInfo.getVersion())
                    .withFileSystemConfigurationView(fileSystemConfigurationView)
                    .withGeneralClusterConfigs(generalClusterConfigs)
                    .withSmartSenseSubscriptionId(smartsenseSubscriptionId)
                    .withLdapConfig(ldapConfig)
                    .withKerberosConfig(kerberosConfig)
                    .build();
        } catch (TemplateProcessingException e) {
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
            fileSystemConfigurationView = new FileSystemConfigurationView(fileSystemConfiguration, source.getCluster().getFileSystem().isDefaultFs());
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
