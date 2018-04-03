package com.sequenceiq.cloudbreak.converter.v2;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.blueprint.BlueprintPreparationObject;
import com.sequenceiq.cloudbreak.blueprint.BlueprintProcessingException;
import com.sequenceiq.cloudbreak.blueprint.GeneralClusterConfigsProvider;
import com.sequenceiq.cloudbreak.blueprint.filesystem.FileSystemConfigurationProvider;
import com.sequenceiq.cloudbreak.blueprint.nifi.HdfConfigProvider;
import com.sequenceiq.cloudbreak.blueprint.nifi.HdfConfigs;
import com.sequenceiq.cloudbreak.blueprint.template.views.BlueprintView;
import com.sequenceiq.cloudbreak.blueprint.template.views.FileSystemConfigurationView;
import com.sequenceiq.cloudbreak.blueprint.templates.BlueprintStackInfo;
import com.sequenceiq.cloudbreak.blueprint.utils.StackInfoService;
import com.sequenceiq.cloudbreak.cloud.model.component.StackRepoDetails;
import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;
import com.sequenceiq.cloudbreak.common.service.user.UserFilterField;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.core.bootstrap.service.container.postgres.PostgresConfigService;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.FileSystem;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.LdapConfig;
import com.sequenceiq.cloudbreak.domain.SmartSenseSubscription;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.service.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.service.ClusterComponentConfigProvider;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.cluster.ambari.InstanceGroupMetadataCollector;
import com.sequenceiq.cloudbreak.service.hostgroup.HostGroupService;
import com.sequenceiq.cloudbreak.service.smartsense.SmartSenseSubscriptionService;
import com.sequenceiq.cloudbreak.service.user.UserDetailsService;

@Component
public class StackToBlueprintPreparationObjectConverter extends AbstractConversionServiceAwareConverter<Stack, BlueprintPreparationObject> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackToBlueprintPreparationObjectConverter.class);

    @Inject
    private HostGroupService hostGroupService;

    @Inject
    private ClusterComponentConfigProvider clusterComponentConfigProvider;

    @Inject
    private UserDetailsService userDetailsService;

    @Inject
    private InstanceGroupMetadataCollector instanceGroupMetadataCollector;

    @Inject
    private SmartSenseSubscriptionService smartSenseSubscriptionService;

    @Inject
    private StackInfoService stackInfoService;

    @Inject
    private HdfConfigProvider hdfConfigProvider;

    @Inject
    private PostgresConfigService postgresConfigService;

    @Inject
    private FileSystemConfigurationProvider fileSystemConfigurationProvider;

    @Inject
    private ClusterService clusterService;

    @Inject
    private GeneralClusterConfigsProvider generalClusterConfigsProvider;

    @Override
    public BlueprintPreparationObject convert(Stack source) {
        try {
            Optional<SmartSenseSubscription> aDefault = smartSenseSubscriptionService.getDefault();
            Cluster cluster = clusterService.getById(source.getCluster().getId());
            FileSystem fileSystem = cluster.getFileSystem();
            LdapConfig ldapConfig = cluster.getLdapConfig();
            StackRepoDetails hdpRepo = clusterComponentConfigProvider.getHDPRepo(cluster.getId());
            String stackRepoDetailsHdpVersion = hdpRepo != null ? hdpRepo.getHdpVersion() : null;
            Map<String, List<InstanceMetaData>> groupInstances = instanceGroupMetadataCollector.collectMetadata(source);
            HdfConfigs hdfConfigs = hdfConfigProvider.createHdfConfig(cluster.getHostGroups(), groupInstances, cluster.getBlueprint().getBlueprintText());
            BlueprintStackInfo blueprintStackInfo = stackInfoService.blueprintStackInfo(cluster.getBlueprint().getBlueprintText());
            FileSystemConfigurationView fileSystemConfigurationView = null;
            if (source.getCluster().getFileSystem() != null) {
                fileSystemConfigurationView = new FileSystemConfigurationView(
                        fileSystemConfigurationProvider.fileSystemConfiguration(fileSystem, source),
                        fileSystem == null ? false : fileSystem.isDefaultFs());
            }
            IdentityUser identityUser = userDetailsService.getDetails(cluster.getOwner(), UserFilterField.USERID);


            return BlueprintPreparationObject.Builder.builder()
                    .withFlexSubscription(source.getFlexSubscription())
                    .withRdsConfigs(postgresConfigService.createRdsConfigIfNeeded(source, cluster))
                    .withHostgroups(hostGroupService.getByCluster(cluster.getId()))
                    .withGateway(cluster.getGateway())
                    .withBlueprintView(new BlueprintView(cluster, blueprintStackInfo))
                    .withStackRepoDetailsHdpVersion(stackRepoDetailsHdpVersion)
                    .withFileSystemConfigurationView(fileSystemConfigurationView)
                    .withGeneralClusterConfigs(generalClusterConfigsProvider.generalClusterConfigs(source, cluster, identityUser))
                    .withSmartSenseSubscriptionId(aDefault.isPresent() ? aDefault.get().getSubscriptionId() : null)
                    .withLdapConfig(ldapConfig)
                    .withHdfConfigs(hdfConfigs)
                    .withKerberosConfig(cluster.isSecure() ? cluster.getKerberosConfig() : null)
                    .build();
        } catch (BlueprintProcessingException e) {
            throw new CloudbreakServiceException(e.getMessage(), e);
        } catch (IOException e) {
            throw new CloudbreakServiceException(e.getMessage(), e);
        }
    }


}
